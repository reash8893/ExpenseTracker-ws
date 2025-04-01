package com.org.test;

import com.org.model.BankTransaction;
import com.org.service.PDFParserService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Simple test class for PDF parsing
 */
public class PDFParserTest {

    public static void main(String[] args) {
        String pdfFilePath;
        
        if (args.length == 0) {
            // Default PDF path if no arguments provided
            pdfFilePath = "D:/Development/ExpenseTracker/Acct Statement_XX4820_10032025.pdf";
            System.out.println("Using default PDF file path: " + pdfFilePath);
        } else {
            pdfFilePath = args[0];
        }
        
        // First, print the raw PDF text
        printRawPDFText(pdfFilePath);
        
        // Then test the parser
        testParser(pdfFilePath);
    }
    
    /**
     * Prints the raw text extracted from a PDF file
     * @param pdfFilePath Path to the PDF file
     */
    private static void printRawPDFText(String pdfFilePath) {
        try {
            File pdfFile = new File(pdfFilePath);
            if (!pdfFile.exists()) {
                System.out.println("PDF file not found: " + pdfFilePath);
                return;
            }
            
            try (PDDocument document = PDDocument.load(pdfFile)) {
                PDFTextStripper textStripper = new PDFTextStripper();
                String text = textStripper.getText(document);
                
                System.out.println("\n\n============== RAW EXTRACTED TEXT FROM PDF ==============\n");
                System.out.println(text);
                System.out.println("\n============== END OF RAW EXTRACTED TEXT ==============\n\n");
            }
        } catch (IOException e) {
            System.out.println("Error reading PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test the rule-based parser with a PDF file
     * @param pdfFilePath Path to the PDF file
     */
    private static void testParser(String pdfFilePath) {
        try {
            PDFParserService parserService = new PDFParserService();
            List<BankTransaction> transactions = parserService.parseLocalPDF(pdfFilePath);
            
            System.out.println("Found " + transactions.size() + " transactions:");
            int i = 1;
            for (BankTransaction tx : transactions) {
                System.out.println("\nTransaction #" + i++ + ":");
                System.out.println("    \"date\": \"" + tx.getDate() + "\",");
                System.out.println("    \"narration\": \"" + tx.getNarration() + "\",");
                System.out.println("    \"chqRefNo\": \"" + tx.getChqRefNo() + "\",");
                System.out.println("    \"valueDate\": \"" + tx.getValueDate() + "\",");
                System.out.println("    \"withdrawalAmt\": " + tx.getWithdrawalAmt() + ",");
                System.out.println("    \"depositAmt\": " + tx.getDepositAmt() + ",");
                System.out.println("    \"closingBalance\": " + tx.getClosingBalance() + ",");
            }
        } catch (IOException e) {
            System.out.println("Error parsing PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 