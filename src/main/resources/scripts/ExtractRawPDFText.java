import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Standalone class to extract raw text from a PDF file
 * This doesn't depend on the ExpenseTracker application classes
 */
public class ExtractRawPDFText {
    
    public static void main(String[] args) {
        // Default PDF path - update this to your PDF location
        String pdfFilePath = "D:/Development/ExpenseTracker/Acct Statement_XX4820_10032025.pdf";
        
        if (args.length > 0) {
            pdfFilePath = args[0];
        }
        
        System.out.println("Extracting raw text from: " + pdfFilePath);
        
        try {
            // Check if file exists
            File pdfFile = new File(pdfFilePath);
            if (!pdfFile.exists()) {
                System.out.println("PDF file not found: " + pdfFilePath);
                return;
            }
            
            // Load PDF and extract text
            try (PDDocument document = PDDocument.load(pdfFile)) {
                PDFTextStripper textStripper = new PDFTextStripper();
                String text = textStripper.getText(document);
                
                // Print to console
                System.out.println("\n============== RAW EXTRACTED TEXT FROM PDF ==============\n");
                System.out.println(text);
                System.out.println("\n============== END OF RAW EXTRACTED TEXT ==============\n");
                
                // Also save to file for easier reference
                String outputFilePath = pdfFilePath + ".txt";
                try (FileWriter writer = new FileWriter(outputFilePath)) {
                    writer.write(text);
                }
                
                System.out.println("Raw text also saved to: " + outputFilePath);
            }
        } catch (IOException e) {
            System.out.println("Error reading PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 