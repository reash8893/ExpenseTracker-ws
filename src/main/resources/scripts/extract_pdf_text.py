"""
Extract raw text from a PDF file
This script will print the raw text from a PDF file before any processing
"""

import os
import sys
import argparse
import pdfplumber  # This is likely already installed if you're using AI parser

def extract_text_from_pdf(pdf_path):
    """
    Extract and print raw text from a PDF file
    """
    if not os.path.exists(pdf_path):
        print(f"Error: File not found: {pdf_path}")
        return
    
    print(f"\n\n============== RAW EXTRACTED TEXT FROM PDF: {pdf_path} ==============\n")
    
    try:
        # Open the PDF with pdfplumber
        with pdfplumber.open(pdf_path) as pdf:
            # Extract text from each page
            for i, page in enumerate(pdf.pages):
                text = page.extract_text()
                print(f"\n----- PAGE {i+1} -----\n")
                print(text)
                
                # Also extract table data if any
                tables = page.extract_tables()
                if tables:
                    print(f"\n----- TABLES ON PAGE {i+1} -----\n")
                    for j, table in enumerate(tables):
                        print(f"Table {j+1}:")
                        for row in table:
                            print(" | ".join([str(cell) if cell else "" for cell in row]))
        
        # Also save to a text file for easier reference
        output_path = f"{pdf_path}.txt"
        with pdfplumber.open(pdf_path) as pdf:
            with open(output_path, 'w', encoding='utf-8') as f:
                for i, page in enumerate(pdf.pages):
                    text = page.extract_text()
                    f.write(f"\n----- PAGE {i+1} -----\n\n")
                    f.write(text + "\n")
        
        print(f"\nRaw text also saved to: {output_path}")
    
    except Exception as e:
        print(f"Error extracting text: {str(e)}")
    
    print("\n============== END OF RAW EXTRACTED TEXT ==============\n")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Extract raw text from a PDF file")
    parser.add_argument("pdf_path", nargs="?", default="D:/Development/ExpenseTracker/Acct Statement_XX4820_10032025.pdf",
                        help="Path to the PDF file (default: D:/Development/ExpenseTracker/Acct Statement_XX4820_10032025.pdf)")
    
    args = parser.parse_args()
    extract_text_from_pdf(args.pdf_path) 