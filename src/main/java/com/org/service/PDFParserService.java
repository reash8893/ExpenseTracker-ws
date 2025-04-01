package com.org.service;

import com.org.model.BankTransaction;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PDFParserService {
    
    private static final Logger logger = LoggerFactory.getLogger(PDFParserService.class);

    /**
     * Parse a bank statement PDF file and extract transactions
     * @param file The uploaded PDF file
     * @return List of extracted transactions
     */
    public List<BankTransaction> parseBankStatement(MultipartFile file) throws IOException {
        // Convert MultipartFile to temporary File
        File tempFile = File.createTempFile("bank-statement", ".pdf");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(file.getBytes());
        }

        // Parse the PDF
        List<BankTransaction> transactions = new ArrayList<>();
        try (PDDocument document = PDDocument.load(tempFile)) {
            PDFTextStripper textStripper = new PDFTextStripper();
            String text = textStripper.getText(document);
            
            // Print the raw extracted text to the terminal
            System.out.println("\n\n============== RAW EXTRACTED TEXT FROM PDF ==============\n");
            System.out.println(text);
            System.out.println("\n============== END OF RAW EXTRACTED TEXT ==============\n\n");
            
            // Also log it 
            
            // Parse transactions from the extracted text
            transactions = extractTransactions(text);
            
            // Mark transactions as coming from rule-based parser
            for (BankTransaction transaction : transactions) {
                transaction.setSource("rule_based_parser");
            }
        } finally {
            // Clean up temporary file
            tempFile.delete();
        }
        
        return transactions;
    }
    
    /**
     * Parse a local PDF file for testing purposes
     * @param filePath Path to the local PDF file
     * @return List of extracted transactions
     */
    public List<BankTransaction> parseLocalPDF(String filePath) throws IOException {
        File pdfFile = new File(filePath);
        if (!pdfFile.exists()) {
            throw new IOException("PDF file not found: " + filePath);
        }
        
        List<BankTransaction> transactions = new ArrayList<>();
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper textStripper = new PDFTextStripper();
            String text = textStripper.getText(document);
            
            // Print the raw extracted text to the terminal
            System.out.println("\n\n============== RAW EXTRACTED TEXT FROM PDF ==============\n");
            System.out.println(text);
            System.out.println("\n============== END OF RAW EXTRACTED TEXT ==============\n\n");
            
            // Parse transactions from the extracted text
            transactions = extractTransactions(text);
            
            // Mark transactions as coming from rule-based parser
            for (BankTransaction transaction : transactions) {
                transaction.setSource("rule_based_parser");
            }
        }
        
        return transactions;
    }
    
    /**
     * Extract transactions from PDF text
     * @param pdfText The extracted text from the PDF
     * @return List of transactions
     */
    private List<BankTransaction> extractTransactions(String pdfText) {
        List<BankTransaction> transactions = new ArrayList<>();
        
        // Split text into lines
        String[] lines = pdfText.split("\\r?\\n");
        
        // Find the table headers to locate the transaction table
        int startLine = 0;
        int withdrawalColIndex = -1;
        int depositColIndex = -1;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.contains("Date") && line.contains("Narration") && 
                line.contains("Chq /Ref.No.") && line.contains("Value Dt")) {
                
                // Find the indices of "Withdrawal Amt." and "Deposit Amt." to determine column ordering
                withdrawalColIndex = line.indexOf("Withdrawal Amt.");
                depositColIndex = line.indexOf("Deposit Amt.");
                
                startLine = i + 1;
                break;
            }
        }
        
        // Find the end of the transaction table (usually marked by "STATEMENT SUMMARY" section)
        int endLine = lines.length;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.contains("STATEMENT SUMMARY") || 
                (line.contains("Opening Balance") && line.contains("Closing Bal"))) {
                endLine = i;
                break;
            }
        }
        
        // Common patterns
        Pattern datePattern = Pattern.compile("(\\d{2}/\\d{2}/\\d{2})");
        Pattern amountPattern = Pattern.compile("((?:,?\\d+)+\\.\\d{2})");
        // Improved reference pattern to match both 10-digit and 16-digit references - bank uses different formats
        Pattern refNumberPattern = Pattern.compile("(\\d{10,16})");
        // UPI pattern to detect UPI transactions
        Pattern upiPattern = Pattern.compile("UPI-[^\\s]+");
        
        // Create a list of line indexes to skip (header/footer lines)
        List<Integer> skipLines = new ArrayList<>();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            // Generic bank statement header/footer patterns
            if (line.contains("Page No") ||
                line.contains("Statement of account") ||
                line.contains("From :") ||
                line.contains("To :") ||
                line.contains("Account Branch") ||
                line.contains("Address :") ||
                line.contains("JOINT HOLDERS") ||
                line.contains("Nomination") ||
                line.contains("STATEMENT SUMMARY") ||
                line.contains("Opening Balance") ||
                line.contains("Closing Bal") ||
                line.contains("Generated On:") ||
                line.contains("Generated By:") ||
                line.contains("Closing balance includes funds") ||
                line.contains("Branch Code") ||
                line.contains("Account No") ||
                line.contains("IFSC") ||
                line.contains("MICR")) {
                skipLines.add(i);
            }
        }
        
        // Process transaction lines
        int i = startLine;
        while (i < endLine) {
            // Skip header/footer lines
            if (skipLines.contains(i)) {
                i++;
                continue;
            }
            
            String line = lines[i].trim();
            
            // Skip empty lines
            if (line.isEmpty()) {
                i++;
                continue;
            }
            
            // Check if line starts with a date (DD/MM/YY format)
            Matcher dateMatcher = datePattern.matcher(line);
            if (dateMatcher.find() && dateMatcher.start() == 0) {
                String date = dateMatcher.group(1);
                
                // Start a new transaction
                StringBuilder narrationBuilder = new StringBuilder();
                String chqRefNo = "";
                String valueDate = "";
                BigDecimal withdrawalAmt = null;
                BigDecimal depositAmt = null;
                BigDecimal closingBalance = null;
                
                // Extract narration - it starts after the date
                String narrationPart = line.substring(date.length()).trim();
                narrationBuilder.append(narrationPart);
                
                // Extract transaction details by finding all lines for this transaction
                List<String> transactionLines = new ArrayList<>();
                transactionLines.add(line);
                
                int j = i + 1;
                boolean possibleContinuation = true;
                while (j < endLine && possibleContinuation) {
                    // Stop if we hit the next transaction (starts with date)
                    String nextLine = lines[j].trim();
                    if (nextLine.isEmpty() || skipLines.contains(j)) {
                        j++;
                        continue;
                    }
                    
                    Matcher nextDateMatcher = datePattern.matcher(nextLine);
                    if (nextDateMatcher.find() && nextDateMatcher.start() == 0) {
                        // Check if the current transaction is incomplete (missing closing balance)
                        // Don't break if the current transaction needs more data
                        if (amountPattern.matcher(String.join(" ", transactionLines)).results().count() >= 2) {
                            break; // We have enough data, safe to break
                        }
                    }
                    
                    // Special handling for UPI transactions - sometimes split across lines
                    Matcher upiMatcher = upiPattern.matcher(nextLine);
                    boolean hasUpi = upiMatcher.find();
                    boolean hasDate = nextDateMatcher.find() && nextDateMatcher.start() == 0;
                    
                    // More aggressive continuation detection for UPI transactions
                    if ((hasUpi && !hasDate) || 
                        (!hasDate && !nextLine.matches("\\d{2}/\\d{2}/\\d{2}.*")) ||
                        (nextLine.contains("@") && !hasDate)) {
                        possibleContinuation = true; // This line could be part of the current transaction
                    } else if (hasDate) {
                        possibleContinuation = false; // Definitely a new transaction
                    }
                    
                    // Add this line to the transaction
                    transactionLines.add(nextLine);
                    
                    // Also append to narration for completeness
                    narrationBuilder.append(" ").append(nextLine);
                    
                    j++;
                }
                
                // Update outer loop index to skip processed lines
                i = j;
                
                // Join all transaction lines for easier processing
                String fullTransactionText = String.join(" ", transactionLines);
                
                // Extract reference number - now handle 10-digit references too
                Matcher refMatcher = refNumberPattern.matcher(fullTransactionText);
                List<String> allRefNumbers = new ArrayList<>();
                while (refMatcher.find()) {
                    allRefNumbers.add(refMatcher.group(1));
                }
                
                // Choose the most appropriate reference number
                // For UPI transactions, prefer numbers starting with "0000" or "1000"
                if (fullTransactionText.contains("UPI-")) {
                    for (String ref : allRefNumbers) {
                        if (ref.startsWith("0000") || ref.startsWith("1000") || ref.startsWith("5000")) {
                            chqRefNo = ref;
                            break;
                        }
                    }
                    // If we didn't find a preferred format, use the longest reference number
                    if (chqRefNo.isEmpty() && !allRefNumbers.isEmpty()) {
                        chqRefNo = allRefNumbers.stream()
                            .max(Comparator.comparing(String::length))
                            .orElse(allRefNumbers.get(0));
                    }
                } else if (!allRefNumbers.isEmpty()) {
                    // For non-UPI, use the first reference number found
                    chqRefNo = allRefNumbers.get(0);
                }
                
                // Extract value date - typically appears after the reference number
                if (!chqRefNo.isEmpty()) {
                    int refIndex = fullTransactionText.indexOf(chqRefNo) + chqRefNo.length();
                    if (refIndex < fullTransactionText.length()) {
                        String afterRef = fullTransactionText.substring(refIndex);
                        Matcher valueDateMatcher = datePattern.matcher(afterRef);
                        if (valueDateMatcher.find()) {
                            valueDate = valueDateMatcher.group(1);
                        }
                    }
                }
                
                // Extract all amounts from the transaction
                List<BigDecimal> amounts = new ArrayList<>();
                List<String> amountStrings = new ArrayList<>(); // Store original amount strings
                
                Matcher amountMatcher = amountPattern.matcher(fullTransactionText);
                while (amountMatcher.find()) {
                    String amountStr = amountMatcher.group(1);
                    amountStrings.add(amountStr); // Keep original format for cleaning later
                    amounts.add(new BigDecimal(amountStr.replace(",", "")));
                }
                
                // Determine which amounts are withdrawal, deposit, and closing balance
                if (amounts.size() >= 3) {
                    // The last amount is almost always the closing balance
                    closingBalance = amounts.get(amounts.size() - 1);
                    
                    // Use context to determine if this is a withdrawal or deposit
                    boolean isLikelyWithdrawal = isWithdrawalTransaction(narrationBuilder.toString(), fullTransactionText);
                    
                    if (isLikelyWithdrawal) {
                        // This is likely a withdrawal transaction
                        withdrawalAmt = amounts.get(0);
                        // Only set deposit if there's explicit deposit evidence
                        if (amounts.size() > 3 && (fullTransactionText.toUpperCase().contains("DEPOSIT") || 
                            fullTransactionText.toUpperCase().contains("CREDIT") || 
                            fullTransactionText.toUpperCase().contains("REFUND") || 
                            fullTransactionText.toUpperCase().contains("REVERSAL"))) {
                            depositAmt = amounts.get(1);
                        }
                    } else {
                        // This is likely a deposit transaction
                        depositAmt = amounts.get(0);
                        // Only set withdrawal if there's explicit withdrawal evidence
                        if (amounts.size() > 3 && (fullTransactionText.toUpperCase().contains("WITHDRAWAL") || 
                            fullTransactionText.toUpperCase().contains("DEBIT") || 
                            fullTransactionText.toUpperCase().contains("CHARGE") || 
                            fullTransactionText.toUpperCase().contains("FEE"))) {
                            withdrawalAmt = amounts.get(1);
                        }
                    }
                    
                    // A transaction should typically have either withdrawal OR deposit, not both
                    // Unless explicitly mentioned as a split transaction
                    if (withdrawalAmt != null && depositAmt != null && 
                        !fullTransactionText.toUpperCase().contains("REVERSAL") && 
                        !fullTransactionText.toUpperCase().contains("REFUND") && 
                        !fullTransactionText.toUpperCase().contains("ADJUSTMENT")) {
                        
                        if (isLikelyWithdrawal) {
                            depositAmt = null; // Clear deposit amount
                        } else {
                            withdrawalAmt = null; // Clear withdrawal amount
                        }
                    }
                } else if (amounts.size() == 2) {
                    // For lines with 2 amounts:
                    // The second amount is typically the closing balance
                    closingBalance = amounts.get(1);
                    
                    // Analyze narration to determine if first amount is withdrawal or deposit
                    boolean isLikelyWithdrawal = isWithdrawalTransaction(narrationBuilder.toString(), fullTransactionText);
                    
                    if (isLikelyWithdrawal) {
                        withdrawalAmt = amounts.get(0);
                        depositAmt = null; // Explicitly clear deposit for UPI transactions
                    } else {
                        depositAmt = amounts.get(0);
                        withdrawalAmt = null; // Explicitly clear withdrawal
                    }
                } else if (amounts.size() == 1) {
                    // Just one amount - check context
                    boolean isLikelyWithdrawal = isWithdrawalTransaction(narrationBuilder.toString(), fullTransactionText);
                    
                    if (fullTransactionText.contains("OPENING BALANCE") || 
                        fullTransactionText.contains("CLOSING BALANCE")) {
                        // This is likely a balance line
                        closingBalance = amounts.get(0);
                    } else if (isLikelyWithdrawal) {
                        withdrawalAmt = amounts.get(0);
                        depositAmt = null; // Explicitly clear deposit
                    } else {
                        depositAmt = amounts.get(0);
                        withdrawalAmt = null; // Explicitly clear withdrawal
                    }
                }
                
                // Clean up narration - remove any header/footer content
                String narration = cleanupNarration(narrationBuilder.toString());
                
                // Remove statement summary content
                narration = removeStatementSummary(narration);
                
                // Extract important descriptive details that might be lost during cleanup
                String importantDetails = extractImportantDescriptiveDetails(fullTransactionText);
                if (!importantDetails.isEmpty() && !narration.contains(importantDetails)) {
                    // Append important details if they're not already in the narration
                    narration = narration + " " + importantDetails;
                }
                
                // If we've extracted transaction amounts and reference numbers,
                // clean them out of the narration text while preserving descriptive content
                if (chqRefNo.length() > 0) {
                    // Remove the reference number
                    narration = narration.replace(chqRefNo, "");
                    
                    // Remove the value date if present
                    if (!valueDate.isEmpty()) {
                        narration = narration.replace(valueDate, "");
                    }
                    
                    // Remove the amount strings
                    for (String amountStr : amountStrings) {
                        narration = narration.replace(amountStr, "");
                    }
                }
                
                // Detect and remove address information but preserve important transaction context
                narration = removeAddressInformation(narration);
                
                // Clean up extra spaces and fix formatting
                narration = narration.replaceAll("\\s+", " ")
                                    .replaceAll("\\s+-\\s+", "-") // Fix hyphenated terms
                                    .replaceAll("--", "-")
                                    .trim();
                
                // Preserve transaction context suffixes (-MOVIE SNACKS, -BAKERY, etc.)
                narration = preserveTransactionContext(narration, fullTransactionText);
                
                // Ensure narration isn't truncated - check for common indicator patterns
                if (narration.endsWith("-") || narration.endsWith("@") || narration.endsWith(".")) {
                    // Look for continuation in the transaction text
                    String[] parts = fullTransactionText.split("\\s+");
                    for (int k = 0; k < parts.length; k++) {
                        if (parts[k].equals(narration.substring(narration.lastIndexOf(" ") + 1))) {
                            // Found the truncation point, try to include more text
                            if (k + 1 < parts.length && !parts[k + 1].matches("\\d+\\.\\d{2}")) {
                                narration += parts[k + 1];
                            }
                        }
                    }
                }
                
                // Special handling for UPI payments - ensure they have good narrations
                if (narration.contains("UPI-")) {
                    narration = cleanupUpiNarration(narration, chqRefNo);
                    
                    // Double-check that this is likely a withdrawal if no explicit classification
                    if (withdrawalAmt == null && depositAmt == null) {
                        withdrawalAmt = amounts.size() > 0 ? amounts.get(0) : null;
                    }
                }
                
                // Build the transaction object
                BankTransaction transaction = BankTransaction.builder()
                    .date(date)
                    .narration(narration)
                    .chqRefNo(chqRefNo)
                    .valueDate(valueDate.isEmpty() ? date : valueDate) // Default to transaction date if missing
                    .withdrawalAmt(withdrawalAmt)
                    .depositAmt(depositAmt)
                    .closingBalance(closingBalance)
                    .flagged(false)
                    .source("rule_based_parser")
                    .build();
                
                transactions.add(transaction);
            } else {
                // If this line doesn't start with a date but contains a UPI transaction
                // marker and a reference number, it might be a split transaction
                if ((line.contains("UPI-") || line.contains("@")) && refNumberPattern.matcher(line).find()) {
                    // Look for the previous line that might contain a date
                    boolean foundTransaction = false;
                    for (int k = i-1; k >= Math.max(0, i-3) && !foundTransaction; k--) {
                        String prevLine = lines[k].trim();
                        Matcher prevDateMatcher = datePattern.matcher(prevLine);
                        if (prevDateMatcher.find()) {
                            // This could be a continuation of a transaction
                            // Combine lines and parse again
                            String combinedLine = prevLine + " " + line;
                            
                            // Attempt to parse this as a transaction
                            String date = prevDateMatcher.group(1);
                            
                            // Extract UPI details - either UPI- marker or @ symbol
                            String narration = "";
                            if (line.contains("UPI-")) {
                                narration = line.substring(line.indexOf("UPI-"));
                            } else if (line.contains("@")) {
                                // Find the word containing @ and start from there
                                String[] words = line.split("\\s+");
                                for (String word : words) {
                                    if (word.contains("@")) {
                                        int wordIndex = line.indexOf(word);
                                        // Look for a few words before to get context
                                        int contextStart = Math.max(0, wordIndex - 20);
                                        narration = line.substring(contextStart);
                                        break;
                                    }
                                }
                            }
                            
                            // If we didn't extract a narration, use the whole line
                            if (narration.isEmpty()) {
                                narration = line;
                            }
                            
                            // Extract reference number
                            Matcher refMatcher = refNumberPattern.matcher(combinedLine);
                            String chqRefNo = refMatcher.find() ? refMatcher.group(1) : "";
                            
                            // Extract amounts
                            Matcher amountMatcher = amountPattern.matcher(combinedLine);
                            List<BigDecimal> amounts = new ArrayList<>();
                            while (amountMatcher.find()) {
                                amounts.add(new BigDecimal(amountMatcher.group(1).replace(",", "")));
                            }
                            
                            // Simple classification - UPI is usually withdrawal unless context suggests deposit
                            BigDecimal withdrawalAmt = null;
                            BigDecimal depositAmt = null;
                            BigDecimal closingBalance = null;
                            
                            boolean isLikelyWithdrawal = isWithdrawalTransaction(narration, combinedLine);
                            
                            if (amounts.size() >= 2) {
                                if (isLikelyWithdrawal) {
                                    withdrawalAmt = amounts.get(0);
                                } else {
                                    depositAmt = amounts.get(0);
                                }
                                closingBalance = amounts.get(amounts.size() - 1);
                            } else if (amounts.size() == 1) {
                                if (isLikelyWithdrawal) {
                                    withdrawalAmt = amounts.get(0);
                                } else {
                                    depositAmt = amounts.get(0);
                                }
                            }
                            
                            // Clean up narration
                            narration = cleanupNarration(narration);
                            
                            // Process specific types of transactions
                            if (narration.contains("UPI-")) {
                                narration = cleanupUpiNarration(narration, chqRefNo);
                            } else {
                                // For non-UPI transactions, ensure we have good context
                                narration = removeAddressInformation(narration);
                                narration = preserveTransactionContext(narration, combinedLine);
                            }
                            
                            // Create transaction object
                            BankTransaction transaction = BankTransaction.builder()
                                .date(date)
                                .narration(narration)
                                .chqRefNo(chqRefNo)
                                .valueDate(date) // Default to transaction date
                                .withdrawalAmt(withdrawalAmt)
                                .depositAmt(depositAmt)
                                .closingBalance(closingBalance)
                                .flagged(false)
                                .source("rule_based_parser")
                                .build();
                            
                            // Add the transaction if it appears valid
                            if ((withdrawalAmt != null || depositAmt != null) && !narration.isEmpty()) {
                                transactions.add(transaction);
                                foundTransaction = true;
                            }
                        }
                    }
                }
                
                // Move to next line
                i++;
            }
        }
        
        // Post-process - validate and correct transactions
        postProcessTransactions(transactions);
        
        return transactions;
    }
    
    /**
     * Remove statement summary content from narration
     */
    private String removeStatementSummary(String narration) {
        // Patterns to remove statement summary content
        String[] summaryPatterns = {
            "STATEMENT SUMMARY[^\\n]*",
            "Opening Balance[^\\n]*",
            "Closing Bal[^\\n]*",
            "Dr Count[^\\n]*",
            "Cr Count[^\\n]*",
            "Debits[^\\n]*",
            "Credits[^\\n]*",
            "Generated On:[^\\n]*",
            "Generated By:[^\\n]*",
            "Requesting[^\\n]*"
        };
        
        String cleaned = narration;
        for (String pattern : summaryPatterns) {
            cleaned = cleaned.replaceAll(pattern, " ");
        }
        
        return cleaned;
    }
    
    /**
     * Extract important descriptive details that should be preserved in the narration
     */
    private String extractImportantDescriptiveDetails(String transactionText) {
        // List of generic transaction categories that should be preserved 
        // This is now based on common transaction types rather than specific merchants
        String[] importantPatterns = {
            "GIFT",
            "TICKET",
            "MOVIE",
            "SALARY",
            "DINING",
            "GROCERY",
            "ENTERTAINMENT",
            "TAXI",
            "TRAVEL",
            "FOOD",
            "RESTAURANT",
            "BILL PAYMENT",
            "RENT",
            "SHOPPING",
            "TRANSFER",
            "INVESTMENT"
        };
        
        StringBuilder details = new StringBuilder();
        
        for (String pattern : importantPatterns) {
            if (transactionText.toUpperCase().contains(pattern) && 
                !details.toString().toUpperCase().contains(pattern)) {
                if (details.length() > 0) {
                    details.append(" ");
                }
                details.append(pattern);
            }
        }
        
        return details.toString();
    }
    
    /**
     * Post-process transactions to ensure logical consistency
     */
    private void postProcessTransactions(List<BankTransaction> transactions) {
        // Sort transactions by date
        transactions.sort((a, b) -> a.getDate().compareTo(b.getDate()));
        
        // Make a second pass to verify and correct withdrawal/deposit classification
        BigDecimal previousBalance = null;
        
        for (int i = 0; i < transactions.size(); i++) {
            BankTransaction tx = transactions.get(i);
            BigDecimal currentBalance = tx.getClosingBalance();
            
            if (currentBalance != null && previousBalance != null) {
                // Calculate the expected change in balance
                BigDecimal expectedChange = currentBalance.subtract(previousBalance);
                
                // Withdrawal decreases balance, deposit increases balance
                BigDecimal actualChange = BigDecimal.ZERO;
                if (tx.getWithdrawalAmt() != null) {
                    actualChange = actualChange.subtract(tx.getWithdrawalAmt());
                }
                if (tx.getDepositAmt() != null) {
                    actualChange = actualChange.add(tx.getDepositAmt());
                }
                
                // If the actual change doesn't match expected change, we need to swap withdrawal and deposit
                if (!expectedChange.equals(actualChange)) {
                    // Swap withdrawal and deposit
                    BigDecimal temp = tx.getWithdrawalAmt();
                    tx.setWithdrawalAmt(tx.getDepositAmt());
                    tx.setDepositAmt(temp);
                }
            }
            
            previousBalance = currentBalance;
        }
        
        // Final pass to check for and fix any truncated narrations
        for (BankTransaction tx : transactions) {
            // Check for truncated narrations ending with certain characters
            String narration = tx.getNarration();
            if (narration.endsWith("-") || narration.endsWith("@") || narration.endsWith(".")) {
                // Try to find the full text from other similar transactions
                for (BankTransaction otherTx : transactions) {
                    if (otherTx != tx && otherTx.getNarration().startsWith(narration.substring(0, narration.length() - 1))) {
                        // Found a more complete narration
                        tx.setNarration(otherTx.getNarration());
                        break;
                    }
                }
            }
        }
    }
    
    /**
     * Determine if a transaction is likely a withdrawal based on narration content
     * Using generic transaction patterns rather than specific vendor names
     */
    private boolean isWithdrawalTransaction(String narration, String fullText) {
        // Keywords that typically indicate withdrawals - generic patterns
        String[] withdrawalKeywords = {
            "UPI-", "PAYMENT", "PURCHASE", "WITHDRAWAL", "DEBIT", "FEE", "CHARGE", 
            "BILL", "TRANSFER TO", "ATM", "POS", "TICKET", "SHOP", "STORE",
            "RESTAURANT", "HOTEL", "TRAVEL", "TAXI", "BAKERY", "ELECTRICAL"
        };
        
        // Keywords that typically indicate deposits - generic patterns
        String[] depositKeywords = {
            "CREDIT", "INTEREST", "DEPOSIT", "REFUND", "REVERSAL", "CASHBACK", 
            "TRANSFER FROM", "SALARY", "INCOME", "RECEIVED"
        };
        
        // UPI transactions are almost always withdrawals unless explicitly mentioned as credits
        if (narration.toUpperCase().contains("UPI-") || fullText.toUpperCase().contains("UPI-")) {
            // Only consider it a deposit if explicitly mentioned as a credit/refund/salary
            if (narration.toUpperCase().contains("CREDIT") || 
                narration.toUpperCase().contains("REFUND") || 
                narration.toUpperCase().contains("REVERSAL") || 
                narration.toUpperCase().contains("SALARY") || 
                narration.toUpperCase().contains("CASHBACK") ||
                fullText.toUpperCase().contains("CREDIT") || 
                fullText.toUpperCase().contains("REFUND") || 
                fullText.toUpperCase().contains("REVERSAL") || 
                fullText.toUpperCase().contains("SALARY") || 
                fullText.toUpperCase().contains("CASHBACK")) {
                return false; // This is likely a deposit
            }
            return true; // Most UPI transactions are withdrawals
        }
        
        // Check for withdrawal keywords
        for (String keyword : withdrawalKeywords) {
            if (narration.toUpperCase().contains(keyword.toUpperCase()) || 
                fullText.toUpperCase().contains(keyword.toUpperCase())) {
                return true;
            }
        }
        
        // Check for deposit keywords
        for (String keyword : depositKeywords) {
            if (narration.toUpperCase().contains(keyword.toUpperCase()) || 
                fullText.toUpperCase().contains(keyword.toUpperCase())) {
                return false;
            }
        }
        
        // Look for structural patterns specific to this bank's format
        if (narration.toUpperCase().startsWith("BY TRANSFER") || 
            narration.toUpperCase().startsWith("TO ")) {
            return false;
        }
        
        // If no keywords matched, use balance-based inference in post-processing
        return true; // Default assumption for UPI and most transactions
    }
    
    /**
     * Clean up a narration by removing header/footer content
     * Using generic bank statement patterns rather than specific personal details
     */
    private String cleanupNarration(String narration) {
        // List of generic patterns to remove
        String[] patterns = {
            "Page No[^\\n]*",
            "Statement of account[^\\n]*",
            "STATEMENT SUMMARY[^\\n]*",
            "From :[^\\n]*",
            "To :[^\\n]*",
            "Account Branch[^\\n]*",
            "Address :[^\\n]*",
            "City :[^\\n]*",
            "State :[^\\n]*",
            "Phone no\\.[^\\n]*",
            "OD Limit[^\\n]*",
            "Currency :[^\\n]*",
            "Email :[^\\n]*",
            "Cust ID[^\\n]*",
            "Account No[^\\n]*",
            "Open Date[^\\n]*",
            "Account Status[^\\n]*",
            "RTGS\\/NEFT IFSC[^\\n]*",
            "MICR :[^\\n]*",
            "Branch Code[^\\n]*",
            "Product Code[^\\n]*",
            "\\*Closing balance[^\\n]*",
            "Contents of this statement[^\\n]*",
            "The address on this statement[^\\n]*",
            "State account branch[^\\n]*",
            "GSTN:[^\\n]*",
            "GSTIN[^\\n]*",
            "https?://www\\.[^\\s]+[^\\n]*",
            "Registered Office Address[^\\n]*",
            "Opening Balance[^\\n]*",
            "Dr Count[^\\n]*",
            "Cr Count[^\\n]*",
            "Debits[^\\n]*",
            "Credits[^\\n]*",
            "Closing Bal[^\\n]*",
            "Generated On:[^\\n]*",
            "Generated By:[^\\n]*",
            "Requesting[^\\n]*",
            "JOINT HOLDERS[^\\n]*",
            "Nomination[^\\n]*"
        };
        
        String cleaned = narration;
        for (String pattern : patterns) {
            cleaned = cleaned.replaceAll(pattern, " ");
        }
        
        // Clean up multiple spaces
        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        
        return cleaned;
    }
    
    /**
     * Remove address information from narration while preserving transaction context
     */
    private String removeAddressInformation(String narration) {
        // List of address indicators that might appear in Indian addresses
        String[] addressIndicators = {
            "PLOT NO", "APARTMENT", "FLAT", "FLOOR", "ROAD", "STREET", "NAGAR", 
            "MAHALAKSHMI", "TAMIL NADU", "INDIA", "KANCHEEPURAM", "CHENNAI", 
            "BANGALORE", "PIN CODE", "TNAGAR", "HABIBULLAH", "MAANGADU", "MANGADU",
            "COLONY", "VILLAGE", "SECTOR", "DISTRICT", "MANDAL", "TEHSIL",
            "TALUK", "PHASE", "BLOCK", "EXTENSION", "LAYOUT", "ENCLAVE"
        };
        
        // Look for any address indicators
        for (String indicator : addressIndicators) {
            int pos = narration.toUpperCase().indexOf(indicator);
            if (pos > 0) {
                // Only truncate if it's not part of the main transaction (after first 25 chars)
                // and if we've already captured the essential transaction details
                if (pos > 25) {
                    return narration.substring(0, pos).trim();
                }
            }
        }
        
        return narration;
    }
    
    /**
     * Preserve transaction context and suffixes like "-MOVIE SNACKS" in narrations
     */
    private String preserveTransactionContext(String narration, String fullText) {
        // Common transaction context patterns that should be preserved
        String[] contextPatterns = {
            "-MOVIE", "-FOOD", "-SNACK", "-BAKERY", "-TICKET", "-PAYMENT",
            "PAYMENT ON", "PAYMENT FOR", "PAYMENT TO", "TRANSFER TO", "TRANSFER FROM",
            "BILL FOR", "PURCHASE OF"
        };
        
        // Check if fullText contains any context patterns that are missing from narration
        for (String pattern : contextPatterns) {
            if (fullText.toUpperCase().contains(pattern) && 
                !narration.toUpperCase().contains(pattern)) {
                
                // Find the context in the full text
                int patternIndex = fullText.toUpperCase().indexOf(pattern);
                if (patternIndex >= 0) {
                    // Extract the context phrase
                    int endIndex = patternIndex + pattern.length();
                    // Include a few more words for context
                    String[] words = fullText.substring(patternIndex).split("\\s+");
                    String context = "";
                    for (int i = 0; i < Math.min(3, words.length); i++) {
                        context += words[i] + " ";
                    }
                    
                    // Append to narration if it makes sense
                    if (!context.isEmpty() && !narration.toUpperCase().contains(context.toUpperCase())) {
                        narration = narration.trim() + " " + context.trim();
                    }
                }
            }
        }
        
        return narration;
    }
    
    /**
     * Clean up UPI transaction narrations to ensure they have all the important details
     */
    private String cleanupUpiNarration(String narration, String refNo) {
        // For UPI transactions, ensure we keep relevant parts
        if (!narration.contains("UPI-")) {
            return narration;
        }
        
        // Check for common UPI context suffixes that should be preserved
        String[] upiContextSuffixes = {
            "-BAKERY", "-MOVIE SNACKS", "-MOVIE", "-SNACK", "-FOOD", "-TICKET", 
            "-PAYMENT", "-GROCERY", "-RESTAURANT", "-UPI", "-HOUSE WARMING G IFT",
            "-HOUSE WARMING", "-GIFT", "-PAYMENT ON CRED", "-UPIINTENT"
        };
        
        String contextSuffix = "";
        for (String suffix : upiContextSuffixes) {
            if (narration.toUpperCase().contains(suffix.toUpperCase())) {
                contextSuffix = suffix;
                break;
            }
        }
        
        // Extract the core UPI details
        Pattern upiFullPattern = Pattern.compile("UPI-([^-]+)-([^@]+@[^-]+)(?:-([^-]+))?(?:-.*)?");
        Matcher upiMatcher = upiFullPattern.matcher(narration);
        
        if (upiMatcher.find()) {
            // We have a complete or partial UPI reference, let's rebuild it properly
            StringBuilder enhancedNarration = new StringBuilder("UPI-");
            
            // Add merchant/recipient name
            enhancedNarration.append(upiMatcher.group(1).trim());
            
            // Add UPI ID if present
            if (upiMatcher.group(2) != null) {
                enhancedNarration.append("-").append(upiMatcher.group(2).trim());
            }
            
            // Add bank code if present
            if (upiMatcher.groupCount() >= 3 && upiMatcher.group(3) != null) {
                enhancedNarration.append("-").append(upiMatcher.group(3).trim());
            }
            
            // Add context suffix if not already included
            if (!contextSuffix.isEmpty() && !enhancedNarration.toString().toUpperCase().contains(contextSuffix.toUpperCase())) {
                enhancedNarration.append(" ").append(contextSuffix);
            }
            
            return enhancedNarration.toString();
        }
        
        // If we have partial UPI information, try to clean it up
        // Extract the merchant name
        Pattern merchantPattern = Pattern.compile("UPI-([^-@\\s]+)");
        Matcher merchantMatcher = merchantPattern.matcher(narration);
        
        if (merchantMatcher.find()) {
            String merchant = merchantMatcher.group(1).trim();
            
            // Extract the UPI ID if present
            Pattern upiIdPattern = Pattern.compile("([\\w.]+@[\\w.]+)");
            Matcher upiIdMatcher = upiIdPattern.matcher(narration);
            String upiId = upiIdMatcher.find() ? upiIdMatcher.group(1) : "";
            
            // Build a clean narration
            String cleanNarration = "UPI-" + merchant;
            if (!upiId.isEmpty()) {
                cleanNarration += "-" + upiId;
            }
            
            // Attempt to extract bank code
            Pattern bankCodePattern = Pattern.compile("@[\\w]+-([A-Z0-9]{4,})");
            Matcher bankCodeMatcher = bankCodePattern.matcher(narration);
            if (bankCodeMatcher.find()) {
                cleanNarration += "-" + bankCodeMatcher.group(1);
            }
            
            // Add context suffix if present
            if (!contextSuffix.isEmpty() && !cleanNarration.toUpperCase().contains(contextSuffix.toUpperCase())) {
                cleanNarration += " " + contextSuffix;
            }
            
            // Add payment context if present and not already added
            if (narration.toUpperCase().contains("PAYMENT") && !cleanNarration.toUpperCase().contains("PAYMENT")) {
                cleanNarration += " payment";
            } else if (narration.toUpperCase().contains("BAKERY") && !cleanNarration.toUpperCase().contains("BAKERY")) {
                cleanNarration += "-BAKERY";
            }
            
            return cleanNarration;
        }
        
        // If all cleanup attempts fail, return original
        return narration;
    }
} 