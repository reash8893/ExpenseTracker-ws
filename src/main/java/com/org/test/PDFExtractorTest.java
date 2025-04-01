package com.org.test;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple standalone test class to extract transactions from HDFC PDF
 */
public class PDFExtractorTest {
    public static void main(String[] args) {
        try {
            // Path to PDF file - adjust as needed
            String pdfFilePath = "D:/Development/ExpenseTracker/Acct Statement_XX4820_10032025.pdf";
            
            // Check if file exists
            File pdfFile = new File(pdfFilePath);
            if (!pdfFile.exists()) {
                System.err.println("PDF file not found: " + pdfFilePath);
                return;
            }
            
            System.out.println("Testing PDF extraction from: " + pdfFilePath);
            
            // Extract text from PDF
            String extractedText = extractTextFromPDF(pdfFile);
            System.out.println("\nExtracted " + extractedText.length() + " characters of text from PDF");
            
            // Print sample of extracted text
            int sampleSize = Math.min(1000, extractedText.length());
            System.out.println("\nSample of extracted text:");
            System.out.println(extractedText.substring(0, sampleSize) + "...");
            
            // Extract transactions from text
            List<String> transactions = extractTransactions(extractedText);
            System.out.println("\nExtracted " + transactions.size() + " potential transactions");
            
            // Print sample of extracted transactions
            int transactionSample = Math.min(5, transactions.size());
            if (transactionSample > 0) {
                System.out.println("\nSample extracted transactions:");
                for (int i = 0; i < transactionSample; i++) {
                    System.out.println((i+1) + ": " + transactions.get(i));
                }
            } else {
                System.out.println("\nNo transactions extracted. Possible issues:");
                System.out.println("1. PDF format might not be readable (image-based or scanned)");
                System.out.println("2. Transaction data pattern doesn't match expected format");
                System.out.println("3. Check for unique patterns in your statement and adjust extraction logic");
            }
            
        } catch (Exception e) {
            System.err.println("Error testing PDF extraction: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Extract text from PDF file using PDFBox
     */
    private static String extractTextFromPDF(File pdfFile) throws IOException {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setSortByPosition(true);
            return textStripper.getText(document);
        }
    }
    
    /**
     * Extract potential transactions from text
     */
    private static List<String> extractTransactions(String text) {
        List<String> transactions = new ArrayList<>();
        
        // Split text into lines
        String[] lines = text.split("\\r?\\n");
        System.out.println("PDF contains " + lines.length + " lines of text");
        
        // Find potential transactions (lines that start with dates and contain amounts)
        Pattern datePattern = Pattern.compile("(\\d{2}/\\d{2}/\\d{2})");
        Pattern amountPattern = Pattern.compile("(\\d{1,3}(?:,\\d{3})*\\.\\d{2})");
        
        StringBuilder currentTransaction = new StringBuilder();
        boolean inTransaction = false;
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            if (line.isEmpty()) continue;
            
            Matcher dateMatcher = datePattern.matcher(line);
            Matcher amountMatcher = amountPattern.matcher(line);
            
            if (dateMatcher.find() && amountMatcher.find()) {
                // If we were already in a transaction, save the previous one
                if (inTransaction && currentTransaction.length() > 0) {
                    transactions.add(currentTransaction.toString());
                }
                
                // Start a new transaction
                currentTransaction = new StringBuilder(line);
                inTransaction = true;
            } 
            // If this line doesn't start with a date but we're in a transaction, 
            // it might be a continuation
            else if (inTransaction && !line.contains("HDFC BANK") && 
                     !line.contains("Page") && !line.contains("STATEMENT SUMMARY")) {
                currentTransaction.append(" ").append(line);
            }
        }
        
        // Add the last transaction if there is one
        if (inTransaction && currentTransaction.length() > 0) {
            transactions.add(currentTransaction.toString());
        }
        
        return transactions;
    }
} 