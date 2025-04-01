import os
import sys
import json
import numpy as np
import cv2
import fitz  # PyMuPDF
from PIL import Image
import torch
from transformers import LayoutLMForTokenClassification, LayoutLMTokenizerFast
import pytesseract
from pytesseract import Output
from flask import Flask, request, jsonify
from werkzeug.utils import secure_filename
import re
import time
import pdfplumber
import argparse
import tempfile
import shutil

app = Flask(__name__)

# Configuration
UPLOAD_FOLDER = 'temp_uploads'
ALLOWED_EXTENSIONS = {'pdf'}
MAX_CONTENT_LENGTH = 16 * 1024 * 1024  # 16 MB limit

app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER
app.config['MAX_CONTENT_LENGTH'] = MAX_CONTENT_LENGTH

os.makedirs(UPLOAD_FOLDER, exist_ok=True)

def allowed_file(filename):
    return '.' in filename and filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

class BankStatementParser:
    """AI-powered bank statement parser using open-source libraries"""
    
    def __init__(self):
        self.tokenizer = None
        self.model = None
        # Load lazily to save resources
    
    def contains_date_pattern(self, text):
        """Check if the text contains a date pattern in formats like DD/MM/YY"""
        import re
        # Check for common date formats in Indian bank statements
        date_patterns = [
            r'\d{2}/\d{2}/\d{2}',  # DD/MM/YY
            r'\d{2}-\d{2}-\d{2}',  # DD-MM-YY
            r'\d{2}\.\d{2}\.\d{2}'  # DD.MM.YY
        ]
        
        for pattern in date_patterns:
            if re.search(pattern, text):
                return True
        return False
    
    def load_model(self):
        """Load the LayoutLM model if not already loaded"""
        if self.model is None:
            try:
                # Use a pre-trained LayoutLM model with correct imports
                self.tokenizer = LayoutLMTokenizerFast.from_pretrained("microsoft/layoutlm-base-uncased")
                self.model = LayoutLMForTokenClassification.from_pretrained("microsoft/layoutlm-base-uncased")
                print("LayoutLM model loaded successfully")
            except Exception as e:
                print(f"Error loading LayoutLM model: {e}")
                print("Falling back to OCR-only mode")
    
    def process_pdf(self, pdf_path):
        """Process PDF and extract structured transaction data using a hybrid approach"""
        try:
            print("Starting hybrid parsing approach")
            
            # Step 1: Try fast regex-based parsing first
            print("Attempting fast regex-based parsing...")
            start_time = time.time()
            transactions = self.process_with_regex(pdf_path)
            regex_time = time.time() - start_time
            
            # If regex parsing found a reasonable number of transactions, return them
            if transactions and len(transactions) >= 10:
                print(f"Regex parsing successful: {len(transactions)} transactions found in {regex_time:.2f} seconds")
                return transactions
            else:
                print("Regex parsing found insufficient transactions, falling back to AI parsing")
            
            # Step 2: Fall back to AI-based parsing if regex didn't find enough transactions
            print("Attempting AI-based parsing...")
            start_time = time.time()
            
            # Try to use LayoutLM if available
            self.load_model()
            if self.model is not None:
                print("Using LayoutLM model for extraction")
                transactions = self.process_with_layout(pdf_path)
            else:
                # If LayoutLM not available, use OCR-based extraction
                print("LayoutLM not available, using OCR-only extraction")
                tables_data = self.extract_tables_from_pdf(pdf_path)
                transactions = []
                for table in tables_data:
                    transactions.extend(table['transactions'])
            
            ai_time = time.time() - start_time
            print(f"AI parsing completed: {len(transactions)} transactions found in {ai_time:.2f} seconds")
            
            return transactions
            
        except Exception as e:
            print(f"Error in hybrid PDF processing: {e}")
            import traceback
            traceback.print_exc()
            return []
    
    def process_with_regex(self, pdf_path):
        """Process PDF using fast regex-based parsing"""
        transactions = []
        
        try:
            # Open PDF with pdfplumber (faster than PyMuPDF for text extraction)
            with pdfplumber.open(pdf_path) as pdf:
                for page_num, page in enumerate(pdf.pages):
                    print(f"Regex parsing page {page_num+1}/{len(pdf.pages)}")
                    
                    # Extract text from page
                    text = page.extract_text()
                    if not text:
                        continue
                    
                    # Split text into lines
                    lines = text.split('\n')
                    
                    # Process each line
                    for line in lines:
                        # Skip short lines or lines without digits (unlikely to be transactions)
                        if len(line) < 10 or not any(c.isdigit() for c in line):
                            continue
                        
                        # Check for date pattern
                        if self.contains_date_pattern(line):
                            transaction = self.parse_with_regex(line)
                            if transaction:
                                transactions.append(transaction)
                        # Additional check: Look for lines that may have been split incorrectly but contain transaction data
                        elif re.search(r'UPI-', line) and len(line) > 15:
                            # Try to find nearby lines that might contain the date
                            if lines.index(line) > 0:
                                prev_line = lines[lines.index(line) - 1]
                                if self.contains_date_pattern(prev_line):
                                    # Combine lines for processing
                                    combined_line = prev_line + " " + line
                                    transaction = self.parse_with_regex(combined_line)
                                    if transaction:
                                        transactions.append(transaction)
            
            # Post-process transactions
            if transactions:
                self.post_process_transactions(transactions)
                
                # Remove duplicates
                transactions = self.remove_duplicates(transactions)
                
                # Sort transactions by date for better readability
                transactions.sort(key=lambda x: x['date'])
            
            return transactions
            
        except Exception as e:
            print(f"Error in regex processing: {e}")
            import traceback
            traceback.print_exc()
            return []
    
    def parse_with_regex(self, line):
        """Parse a transaction line using regex patterns"""
        import re
        
        # Define patterns for Indian bank statements
        patterns = {
            'date': r'(\d{2}/\d{2}/\d{2})',  # DD/MM/YY format
            'reference': r'(\d{10,16})',      # 10-16 digit reference numbers
            'amount': r'(\d{1,3}(?:,\d{3})*\.\d{2})',  # Amount with decimals
            'upi': r'(?:UPI|Upi|upi)[-\s]([^-\s].*?(?=\s*(?:@|$|\d|\s{2,})))'  # UPI transaction details
        }
        
        # Extract date
        date_match = re.search(patterns['date'], line)
        if not date_match:
            return None
        
        date = date_match.group(1)
        
        # Full text as narration initially
        narration = line
        
        # Remove date from narration
        narration = re.sub(r'\b' + re.escape(date) + r'\b', '', narration)
        
        # Extract reference number
        ref_match = re.search(patterns['reference'], line)
        ref_no = ref_match.group(1) if ref_match else ""
        
        # Remove reference from narration if found
        if ref_no:
            narration = narration.replace(ref_no, '')
        
        # Extract all amounts
        amounts = []
        amount_matches = re.finditer(patterns['amount'], line)
        for match in amount_matches:
            amounts.append(match.group(1))
            narration = narration.replace(match.group(1), '')
        
        # IMPROVED ADDRESS DETECTION: More comprehensive list and more aggressive detection
        address_indicators = [
            "PLOT NO", "APARTMENT", "FLAT", "FLOOR", "ROAD", "STREET", "NAGAR", 
            "MAHALAKSHMI", "TAMIL NADU", "INDIA", "KANCHEEPURAM", "CHENNAI", 
            "BANGALORE", "PIN CODE", "TNAGAR", "HABIBULLAH", "MAANGADU", "MANGADU",
            # Add more address-related keywords specific to Indian addresses
            "COLONY", "VILLAGE", "SECTOR", "DISTRICT", "MANDAL", "TEHSIL",
            "TALUK", "PHASE", "BLOCK", "EXTENSION", "LAYOUT", "ENCLAVE"
        ]
        
        # Check if the narration contains address information
        for indicator in address_indicators:
            if indicator in narration.upper():
                # Find the position of the first address indicator
                pos = narration.upper().find(indicator)
                # More aggressive cutoff to catch address info (reduced from 30 to 25)
                if pos > 25:
                    # Truncate the narration at this point
                    narration = narration[:pos].strip()
                    break
        
        # Make UPI transaction detection more robust
        is_upi = "UPI-" in narration or " UPI " in narration
        
        # If narration is still very long, may contain other non-transaction text
        if len(narration) > 120:  # Reduced from 150 to catch more non-transaction text
            if is_upi:
                # For UPI transactions, extract structured parts - preserve full UPI reference but remove noise
                upi_id_match = re.search(r'([A-Za-z0-9_.]+@[A-Za-z0-9_.]+)', narration)
                bank_code_match = re.search(r'([A-Z0-9]{10,})', narration)
                
                # Extract complete UPI transaction info (beneficiary, UPI ID, bank code)
                upi_pattern = r'(UPI-[^-]+(?:-[^-]+)?@[^-]+(?:-[A-Z0-9]+)?(?:-\d+)?)'
                upi_match = re.search(upi_pattern, narration)
                
                if upi_match:
                    base_narration = upi_match.group(1)
                    
                    # Try to extract transaction context (like "PAYMENT ON CRED")
                    context_match = re.search(r'-([A-Z][A-Z0-9 ]{5,}(?:ON|FROM|TO|FOR)[A-Z0-9 ]+)', narration, re.IGNORECASE)
                    if context_match:
                        base_narration += " " + context_match.group(1)
                    
                    narration = base_narration
                else:
                    # Default fallback: just truncate
                    narration = narration[:90].strip()
            else:
                # For non-UPI transactions, just truncate to a reasonable length
                narration = narration[:90].strip()
        
        # Create transaction object
        transaction = {
            'date': date,
            'narration': narration.strip(),
            'chqRefNo': ref_no,
            'valueDate': date,  # Set value date to match transaction date
            'withdrawalAmt': None,
            'depositAmt': None,
            'closingBalance': None,
            'flagged': False,
            'source': 'regex_parser'  # Mark as coming from regex parser
        }
        
        # Determine transaction type and assign amounts
        # Strategy: Look for keywords that indicate withdrawal or deposit
        # and use position of amounts to determine which is which
        
        # If we have at least one amount
        if amounts:
            # Try to determine transaction type from keywords in narration
            is_withdrawal = self.is_likely_withdrawal(narration)
            
            if len(amounts) == 1:
                # With just one amount, use the context to determine if it's withdrawal or deposit
                if is_withdrawal > 0:
                    transaction['withdrawalAmt'] = self.parse_amount(amounts[0])
                else:
                    transaction['depositAmt'] = self.parse_amount(amounts[0])
            
            elif len(amounts) == 2:
                # Two amounts: usually one transaction amount and one balance
                # Last amount is typically the balance
                transaction['closingBalance'] = self.parse_amount(amounts[-1])
                
                # First amount is transaction - determine if withdrawal or deposit
                if is_withdrawal > 0:
                    transaction['withdrawalAmt'] = self.parse_amount(amounts[0])
                else:
                    transaction['depositAmt'] = self.parse_amount(amounts[0])
            
            elif len(amounts) >= 3:
                # Three or more amounts: typically withdrawal, deposit, and balance
                # Last is balance, first is withdrawal, second is deposit (typical Indian bank format)
                transaction['withdrawalAmt'] = self.parse_amount(amounts[0])
                transaction['depositAmt'] = self.parse_amount(amounts[1])
                transaction['closingBalance'] = self.parse_amount(amounts[-1])
                
                # Apply specific rules to fix common misclassifications
                self.apply_transaction_specific_rules(transaction)
        
        # Clean up narration
        transaction['narration'] = self.clean_narration_thoroughly(narration)
        
        return transaction
    
    def clean_narration_thoroughly(self, narration):
        """Clean narration text more thoroughly to preserve full content"""
        if not narration:
            return ""
        
        # Remove leading/trailing whitespace
        narration = narration.strip()
        
        # Remove pipe characters completely
        narration = narration.replace("|", "")
        
        # Replace multiple spaces with single space
        narration = re.sub(r'\s+', ' ', narration).strip()
        
        # Don't truncate or over-clean UPI transaction narrations
        # Preserve more details while still ensuring consistency
        if "upi" in narration.lower():
            # Make UPI consistent in capitalization
            narration = re.sub(r'(?i)upi-', 'UPI-', narration)
            narration = re.sub(r'(?i)upi ', 'UPI-', narration)
            
            # For UPI transactions, preserve full details but with consistent format
            # Don't strip out bank codes, reference numbers, or transaction contexts
            # Only remove very long address information if present
            address_indicators = [
                "PLOT NO", "APARTMENT", "FLAT", "FLOOR", "ROAD", "STREET", "NAGAR", 
                "MAHALAKSHMI", "TAMIL NADU", "INDIA", "KANCHEEPURAM", "CHENNAI", 
                "BANGALORE", "PIN CODE", "TNAGAR", "HABIBULLAH"
            ]
            
            for indicator in address_indicators:
                if indicator in narration.upper():
                    pos = narration.upper().find(indicator)
                    if pos > 50:  # Allow for more context before cutting off
                        narration = narration[:pos].strip()
                        break
        else:
            # For non-UPI transactions, apply more cleaning but still preserve most details
            # Only truncate very long descriptions (over 200 chars) that likely contain irrelevant info
            if len(narration) > 200:
                narration = narration[:200].strip()
        
        return narration.strip()
    
    def remove_duplicates(self, transactions):
        """Remove duplicate transactions based on key fields"""
        unique_transactions = {}
        
        for tx in transactions:
            # Create a unique key for each transaction
            tx_key = f"{tx['date']}|{tx['withdrawalAmt']}|{tx['depositAmt']}|{tx['closingBalance']}|{tx['chqRefNo']}"
            
            # Only keep transaction with the most detailed narration
            if tx_key not in unique_transactions or len(tx['narration']) > len(unique_transactions[tx_key]['narration']):
                unique_transactions[tx_key] = tx
        
        return list(unique_transactions.values())
    
    def process_with_layout(self, input_data):
        """
        Parse bank statement using layout analysis of the PDF
        
        Args:
            input_data: Either a string containing PDF text or a file path to a PDF
        
        Returns:
            List of transaction dictionaries
        """
        # If input_data is a file path, extract text from PDF
        if isinstance(input_data, str) and (input_data.endswith('.pdf') and os.path.exists(input_data)):
            with pdfplumber.open(input_data) as pdf:
                text = ''
                for page in pdf.pages:
                    extracted = page.extract_text()
                    if extracted:
                        text += extracted + '\n'
        else:
            # Assume input_data is already extracted text
            text = input_data
            
        # Look for transaction table in the text
        lines = text.split('\n')
        
        # Find header line that indicates where transaction table starts
        table_start = 0
        for i, line in enumerate(lines):
            # Look for header line in bank statement (like "Date Description Ref No Value Date Debit Credit Balance")
            if re.search(r'Date.*Narration.*Chq.*Ref.*Value.*Withdrawal.*Deposit.*Balance', line, re.IGNORECASE):
                table_start = i + 1
                break
        
        # Find where transaction table ends (usually at statement summary)
        table_end = len(lines)
        for i, line in enumerate(lines[table_start:], table_start):
            if re.search(r'(STATEMENT SUMMARY|TOTAL|Closing Balance|Generated On)', line, re.IGNORECASE):
                table_end = i
                break
        
        # Extract only the transaction part of the statement
        transaction_text = '\n'.join(lines[table_start:table_end])
        
        # Process the transactions
        transactions = []
        
        # Use layout-based analysis: look for patterns of date followed by transaction details
        date_pattern = r'(\d{2}/\d{2}/\d{2})'
        
        # Find all dates and use them as transaction separators
        matches = list(re.finditer(date_pattern, transaction_text))
        
        # Process each transaction block
        for i in range(len(matches)):
            start_pos = matches[i].start()
            # Calculate end of this transaction block
            end_pos = matches[i+1].start() if i < len(matches) - 1 else len(transaction_text)
            
            transaction_block = transaction_text[start_pos:end_pos].strip()
            if transaction_block:
                transaction = self.extract_potential_transaction(transaction_block)
                if transaction:
                    transactions.append(transaction)
        
        return transactions
    
    def extract_text_blocks_with_layout(self, ocr_result):
        """Extract text blocks from OCR results with their layout information"""
        text_blocks = []
        
        # Group OCR results into lines based on top coordinate
        line_groups = {}
        for i, conf in enumerate(ocr_result['conf']):
            if float(conf) > 30:  # Only consider text with confidence > 30
                text = ocr_result['text'][i]
                if not text.strip():
                    continue
                    
                top = ocr_result['top'][i]
                line_id = top // 10  # Group by top coordinate with some tolerance
                
                if line_id not in line_groups:
                    line_groups[line_id] = []
                
                line_groups[line_id].append({
                    'text': text,
                    'left': ocr_result['left'][i],
                    'top': top,
                    'width': ocr_result['width'][i],
                    'height': ocr_result['height'][i]
                })
        
        # Sort line groups by vertical position
        sorted_line_ids = sorted(line_groups.keys())
        
        # Group lines into blocks
        current_block = []
        last_line_id = -1
        
        for line_id in sorted_line_ids:
            # If there's a significant gap between lines, start a new block
            if last_line_id >= 0 and (line_id - last_line_id) > 2:
                if current_block:
                    text_blocks.append(current_block)
                    current_block = []
            
            # Sort words in line by horizontal position
            sorted_words = sorted(line_groups[line_id], key=lambda w: w['left'])
            current_block.append(sorted_words)
            last_line_id = line_id
        
        # Add the last block
        if current_block:
            text_blocks.append(current_block)
        
        return text_blocks
    
    def process_layout_blocks(self, text_blocks, img_np):
        """Process text blocks with layout information to extract transactions"""
        transactions = []
        
        # Filter out header blocks using patterns
        header_patterns = ["statement of account", "account number", "from to", "branch code"]
        footer_patterns = ["statement summary", "thank you", "balance carried forward", "page", "**"]
        
        # Lower threshold for detecting transaction blocks
        for block in text_blocks:
            # Reconstruct the text from the block with layout awareness
            block_text = self.reconstruct_block_text(block)
            
            # Skip header and footer blocks
            if any(pattern in block_text.lower() for pattern in header_patterns + footer_patterns):
                print(f"Skipping header/footer block: {block_text[:30]}...")
                continue
                
            # More lenient date pattern detection to catch more transactions
            if self.contains_date_pattern(block_text):
                # More flexible date validation - we want to catch all potential transactions
                # Even if the date format isn't perfect, we can fix it later
                transaction = self.parse_transaction_with_layout(block)
                if transaction:
                    transactions.append(transaction)
            
            # Secondary attempt to catch transactions without typical date patterns
            # Look for rows that have amount patterns and potential transaction indicators
            if self.could_be_transaction(block_text):
                # Try to extract transaction details anyway
                transaction = self.extract_potential_transaction(block)
                if transaction:
                    transactions.append(transaction)
        
        # Post-processing to remove low-confidence transactions but keep more valid ones
        transactions = self.filter_valid_transactions(transactions)
        
        return transactions
    
    def could_be_transaction(self, text):
        """Check if a text block could potentially be a transaction even without obvious date pattern"""
        import re
        # Look for amount patterns (numbers with decimals)
        has_amount = bool(re.search(r'\d{1,3}(?:,\d{3})*\.\d{2}', text))
        
        # Look for common transaction indicators
        transaction_indicators = ["upi", "transfer", "credit", "debit", "payment", "withdraw", "deposit", "bal", "balance"]
        has_indicator = any(indicator in text.lower() for indicator in transaction_indicators)
        
        # Look for reference number patterns
        has_ref = bool(re.search(r'\d{10,}', text))
        
        # If it has at least an amount pattern and either an indicator or reference number, it could be a transaction
        return has_amount and (has_indicator or has_ref)
        
    def extract_potential_transaction(self, block):
        """Try to extract transaction info from a block that doesn't have obvious date pattern"""
        import re
        
        # Extract the full text of the block
        block_text = self.reconstruct_block_text(block)
        
        # Look for a date-like pattern
        date_patterns = [
            r'(\d{2}/\d{2}/\d{2})',  # DD/MM/YY
            r'(\d{2}-\d{2}-\d{2})',  # DD-MM-YY
            r'(\d{2}\.\d{2}\.\d{2})'  # DD.MM.YY
        ]
        
        date = None
        for pattern in date_patterns:
            match = re.search(pattern, block_text)
            if match:
                date = match.group(1)
                # Standardize date format to DD/MM/YY
                date = date.replace('-', '/').replace('.', '/')
                break
        
        # If no date found, try to infer from context or use a placeholder
        if not date:
            # Look for month names
            months = ['jan', 'feb', 'mar', 'apr', 'may', 'jun', 'jul', 'aug', 'sep', 'oct', 'nov', 'dec']
            for i, month in enumerate(months, 1):
                if month in block_text.lower():
                    # If we find a month name, look for nearby numbers that could be day/year
                    day_matches = re.findall(r'\b(\d{1,2})\b', block_text)
                    if day_matches:
                        day = day_matches[0].zfill(2)
                        # Default to current year if not found
                        date = f"{day}/{str(i).zfill(2)}/25"
                        break
            
            # If still no date, skip this transaction
            if not date:
                return None
        
        # For narration, preserve more information from the original text
        narration = block_text
        
        # Remove date from narration
        narration = re.sub(r'\b' + re.escape(date) + r'\b', '', narration)
        
        # Extract reference number and all amount patterns
        ref_match = re.search(r'(\d{10,16})', block_text)
        ref_no = ref_match.group(1) if ref_match else ""
        
        # Extract amounts
        amounts = []
        amount_matches = re.finditer(r'(\d{1,3}(?:,\d{3})*\.\d{2})', block_text)
        for match in amount_matches:
            amounts.append(match.group(1))
            narration = narration.replace(match.group(1), '')
        
        # For UPI transactions, be very careful not to remove important parts
        is_upi = "upi" in narration.lower()
        
        if is_upi:
            # For UPI transactions, try to keep the entire UPI pattern including beneficiary name,
            # UPI ID, bank code, and reference number
            
            # Only remove the reference number if it appears as a standalone number
            if ref_no:
                # Only remove exact matches of the reference number that aren't part of UPI IDs
                narration = re.sub(r'(?<!\w)' + re.escape(ref_no) + r'(?!\w)', '', narration)
            
            # Don't truncate UPI information
        else:
            # For non-UPI transactions, we can be more aggressive in cleaning
            # Remove reference number from narration if found
            if ref_no:
                narration = narration.replace(ref_no, '')
            
            # Remove long address information if present
            address_indicators = [
                "PLOT NO", "APARTMENT", "FLAT", "FLOOR", "ROAD", "STREET", "NAGAR", 
                "MAHALAKSHMI", "TAMIL NADU", "INDIA", "KANCHEEPURAM", "CHENNAI", 
                "BANGALORE", "PIN CODE", "TNAGAR", "HABIBULLAH"
            ]
            
            for indicator in address_indicators:
                if indicator in narration.upper():
                    pos = narration.upper().find(indicator)
                    if pos > 30:
                        narration = narration[:pos].strip()
                        break
            
            # If non-UPI narration is still very long, truncate it
            if len(narration) > 150:
                narration = narration[:150].strip()
        
        # Create transaction object
        transaction = {
            'date': date,
            'narration': self.clean_narration_thoroughly(narration.strip()),
            'chqRefNo': ref_no,
            'valueDate': date,  # Use transaction date as value date
            'withdrawalAmt': None,
            'depositAmt': None,
            'closingBalance': None,
            'flagged': False,
            'source': 'ai_parser'
        }
        
        # Process amounts based on contextual information
        if len(amounts) >= 1:
            transaction['closingBalance'] = self.parse_amount(amounts[-1])
            
        if len(amounts) >= 2:
            # Check the narration to determine if this is likely a withdrawal or deposit
            withdrawal_score = self.is_likely_withdrawal(narration, block_text)
            
            if withdrawal_score > 0:
                transaction['withdrawalAmt'] = self.parse_amount(amounts[0])
            else:
                transaction['depositAmt'] = self.parse_amount(amounts[0])
        
        if len(amounts) >= 3:
            # With 3+ amounts, we have both withdrawal and deposit (and balance)
            transaction['withdrawalAmt'] = self.parse_amount(amounts[0])
            transaction['depositAmt'] = self.parse_amount(amounts[1])
            transaction['closingBalance'] = self.parse_amount(amounts[2])
            
            # Check if we need to swap based on narration context
            withdrawal_score = self.is_likely_withdrawal(narration, block_text)
            
            if withdrawal_score < -10 and transaction['withdrawalAmt'] is not None and transaction['depositAmt'] is None:
                transaction['depositAmt'] = transaction['withdrawalAmt']
                transaction['withdrawalAmt'] = None
            elif withdrawal_score > 10 and transaction['depositAmt'] is not None and transaction['withdrawalAmt'] is None:
                transaction['withdrawalAmt'] = transaction['depositAmt']
                transaction['depositAmt'] = None
        
        return transaction
    
    def filter_valid_transactions(self, transactions):
        """Filter out invalid or low-confidence transactions"""
        valid_transactions = []
        
        for tx in transactions:
            # Less strict filtering to keep more transactions
            # Only skip transactions with no amounts at all
            if tx['withdrawalAmt'] is None and tx['depositAmt'] is None and tx['closingBalance'] is None:
                continue
                
            # Skip transactions with obviously suspicious narrations
            highly_suspicious_terms = ["statement of account", "account number"]
            if any(term in tx['narration'].lower() for term in highly_suspicious_terms):
                continue
            
            # Allow more transactions through - don't filter out on date format alone
            # We'll try to fix the date format
            if not self.is_correct_date_format(tx['date']):
                tx['date'] = self.fix_date_format(tx['date'])
                
            valid_transactions.append(tx)
            
        return valid_transactions
    
    def is_valid_recent_date(self, text):
        """Check if text contains a valid recent date (2020-2029)"""
        import re
        # Look for DD/MM/YY where YY is 20-29 (2020s)
        date_pattern = r'\d{2}/\d{2}/(2[0-9]|[0-9]{2})'
        match = re.search(date_pattern, text)
        if match:
            date_str = match.group(0)
            # If it's a 2-digit year, make sure it's recent
            if len(date_str) == 8:  # DD/MM/YY format
                year = date_str[-2:]
                return '20' <= year <= '29'
            return True
        return False
        
    def is_correct_date_format(self, date_str):
        """Check if date is in correct DD/MM/YY format"""
        import re
        return bool(re.match(r'\d{2}/\d{2}/\d{2}', date_str))
    
    def fix_date_format(self, date_str):
        """Try to fix date format issues"""
        import re
        
        # If format is DD/MM/YYYY, convert to DD/MM/YY
        match = re.match(r'(\d{2})/(\d{2})/(\d{4})', date_str)
        if match:
            day, month, year = match.groups()
            return f"{day}/{month}/{year[-2:]}"
            
        # If format is D/M/YY, convert to DD/MM/YY
        match = re.match(r'(\d{1,2})/(\d{1,2})/(\d{2})', date_str)
        if match:
            day, month, year = match.groups()
            return f"{day.zfill(2)}/{month.zfill(2)}/{year}"
            
        # Handle other date formats like DD-MM-YY
        match = re.match(r'(\d{2})-(\d{2})-(\d{2})', date_str)
        if match:
            day, month, year = match.groups()
            return f"{day}/{month}/{year}"
            
        # Default case - return as is
        return date_str
    
    def reconstruct_block_text(self, block):
        """Reconstruct text from a block of words with layout information"""
        lines = []
        for line in block:
            line_text = ' '.join(word['text'] for word in line)
            lines.append(line_text)
        return '\n'.join(lines)
    
    def parse_transaction_line(self, line):
        """Parse a transaction line into structured data"""
        import re
        
        # Try to extract date
        date_match = re.search(r'(\d{2}/\d{2}/\d{2})', line)
        if not date_match:
            return None
        
        date = date_match.group(1)
        
        # Full text as narration initially - preserve as much as possible
        narration = line
        
        # Remove date from narration
        narration = re.sub(r'\b' + re.escape(date) + r'\b', '', narration)
        
        # Extract reference number (typically 16 digits, but allow 10-16 digits)
        ref_match = re.search(r'(\d{10,16})', line)
        ref_no = ref_match.group(1) if ref_match else ""
        
        # Extract amounts (numbers with decimal points)
        amounts = []
        amount_matches = re.finditer(r'(\d{1,3}(?:,\d{3})*\.\d{2})', line)
        for match in amount_matches:
            amounts.append(match.group(1))
            narration = narration.replace(match.group(1), '')
        
        # For UPI transactions, be very careful not to remove important parts
        is_upi = "upi" in narration.lower()
        
        if is_upi:
            # For UPI transactions, try to keep all UPI details
            # Only remove the reference number if it appears as a standalone number
            if ref_no:
                narration = re.sub(r'(?<!\w)' + re.escape(ref_no) + r'(?!\w)', '', narration)
            
            # Extract and keep the UPI transaction full details - beneficiary, UPI ID, bank code
            # This approach preserves the structured UPI information similar to the rule-based parser
        else:
            # For non-UPI transactions, we can be more aggressive in cleaning
            # Remove reference number from narration if found
            if ref_no:
                narration = narration.replace(ref_no, '')
            
            # Remove long address information if present
            address_indicators = [
                "PLOT NO", "APARTMENT", "FLAT", "FLOOR", "ROAD", "STREET", "NAGAR", 
                "MAHALAKSHMI", "TAMIL NADU", "INDIA", "KANCHEEPURAM", "CHENNAI", 
                "BANGALORE", "PIN CODE", "TNAGAR", "HABIBULLAH"
            ]
            
            for indicator in address_indicators:
                if indicator in narration.upper():
                    pos = narration.upper().find(indicator)
                    if pos > 30:
                        narration = narration[:pos].strip()
                        break
            
            # If non-UPI narration is still very long, truncate it
            if len(narration) > 150:
                narration = narration[:150].strip()
        
        # Create transaction object
        transaction = {
            'date': date,
            'narration': self.clean_narration_thoroughly(narration.strip()),
            'chqRefNo': ref_no,
            'valueDate': date,  # Use transaction date as value date
            'withdrawalAmt': None,
            'depositAmt': None,
            'closingBalance': None,
            'flagged': False,
            'source': 'ai_parser'
        }
        
        # Fill in amounts based on position or keywords
        if len(amounts) >= 3:
            # If we have 3 amounts, they're likely withdrawal, deposit, and balance
            transaction['withdrawalAmt'] = self.parse_amount(amounts[0])
            transaction['depositAmt'] = self.parse_amount(amounts[1])
            transaction['closingBalance'] = self.parse_amount(amounts[2])
            
            # Apply specific rules to fix common misclassifications
            self.apply_transaction_specific_rules(transaction)
        elif len(amounts) == 2:
            # If we have 2 amounts, determine which is which by analyzing narration
            if self.is_likely_withdrawal(narration) > 0:
                transaction['withdrawalAmt'] = self.parse_amount(amounts[0])
            else:
                transaction['depositAmt'] = self.parse_amount(amounts[0])
            
            transaction['closingBalance'] = self.parse_amount(amounts[1])
        elif len(amounts) == 1:
            # Just one amount, likely the closing balance
            transaction['closingBalance'] = self.parse_amount(amounts[0])
        
        return transaction
    
    def parse_amount(self, amount_str):
        """Parse amount string to number"""
        try:
            # Remove commas
            clean_amount = amount_str.replace(",", "")
            return float(clean_amount)
        except ValueError:
            return None
    
    def post_process_transactions(self, transactions):
        """Post-process transactions to fix classifications and balance calculation"""
        if not transactions:
            return
        
        # Sort by date
        transactions.sort(key=lambda x: x['date'])
        
        # Validate withdrawal/deposit based on balance changes
        prev_balance = None
        
        for tx in transactions:
            # Fix specific transaction patterns - UPI with Indian banks frequently follows patterns
            if "upi-" in tx['narration'].lower():
                withdrawal_score = self.is_likely_withdrawal(tx['narration'])
                
                # If strong likelihood of withdrawal but classified as deposit
                if withdrawal_score > 7 and tx['withdrawalAmt'] is None and tx['depositAmt'] is not None:
                    tx['withdrawalAmt'] = tx['depositAmt']
                    tx['depositAmt'] = None
                # If strong likelihood of deposit but classified as withdrawal
                elif withdrawal_score < -7 and tx['depositAmt'] is None and tx['withdrawalAmt'] is not None:
                    tx['depositAmt'] = tx['withdrawalAmt']
                    tx['withdrawalAmt'] = None
            
            # Apply logic based on common transaction names
            self.apply_transaction_specific_rules(tx)
            
            # Balance-based validation
            if tx['closingBalance'] is not None:
                current_balance = tx['closingBalance']
                
                if prev_balance is not None:
                    # Calculate change in balance
                    balance_change = current_balance - prev_balance
                    
                    # Expected change based on transaction
                    expected_change = 0
                    if tx['depositAmt'] is not None:
                        expected_change += tx['depositAmt']
                    if tx['withdrawalAmt'] is not None:
                        expected_change -= tx['withdrawalAmt']
                    
                    # If balance change doesn't match transaction, swap withdrawal and deposit
                    if abs(balance_change - expected_change) > 0.5:  # Allow for small rounding errors
                        print(f"Fixing transaction: {tx['date']} - {tx['narration']} (balance change: {balance_change}, expected: {expected_change})")
                        if balance_change > 0 and tx['withdrawalAmt'] is not None and tx['depositAmt'] is None:
                            # If balance increased but we only have withdrawal, swap them
                            tx['depositAmt'] = tx['withdrawalAmt']
                            tx['withdrawalAmt'] = None
                        elif balance_change < 0 and tx['depositAmt'] is not None and tx['withdrawalAmt'] is None:
                            # If balance decreased but we only have deposit, swap them
                            tx['withdrawalAmt'] = tx['depositAmt']
                            tx['depositAmt'] = None
                
                prev_balance = current_balance
                
    def apply_transaction_specific_rules(self, transaction):
        """Apply specific rules to fix known transaction patterns"""
        narration = transaction['narration'].lower()
        
        # CRED is a payment app, typically a withdrawal
        if "cred" in narration and transaction['depositAmt'] is not None and transaction['withdrawalAmt'] is None:
            transaction['withdrawalAmt'] = transaction['depositAmt']
            transaction['depositAmt'] = None
            
        # Common online shopping platforms are withdrawals
        shopping_platforms = ["amazon", "flipkart", "myntra", "ajio", "zeptonow", "swiggy", "zomato"]
        if any(platform in narration for platform in shopping_platforms) and transaction['depositAmt'] is not None and transaction['withdrawalAmt'] is None:
            transaction['withdrawalAmt'] = transaction['depositAmt']
            transaction['depositAmt'] = None
            
        # Common deposit indicators
        deposit_indicators = ["salary", "interest", "cashback", "refund", "credit"]
        if any(indicator in narration for indicator in deposit_indicators) and transaction['withdrawalAmt'] is not None and transaction['depositAmt'] is None:
            transaction['depositAmt'] = transaction['withdrawalAmt']
            transaction['withdrawalAmt'] = None
            
    def is_likely_withdrawal(self, narration, additional_context=None):
        """Determine if a transaction is likely a withdrawal based on narration text"""
        narration = narration.lower()
        context = (additional_context or "").lower()
        
        # Score-based approach for better accuracy
        score = 0
        
        # Keywords strongly indicating withdrawal
        withdrawal_indicators = ["upi-", "paid", "payment", "purchase", "debit", "withdraw", "withdrawal", "bill", "charge", "cred", "spend", "buy", "amazon", "atm", "cash", "swiggy", "zomato", "shopping"]
        # Keywords strongly indicating deposit
        deposit_indicators = ["credit", "salary", "interest", "cashback", "refund", "return", "reversal", "income", "deposit"]
        
        # Check withdrawal indicators
        for word in withdrawal_indicators:
            if word in narration or word in context:
                score += 3
        
        # Check deposit indicators
        for word in deposit_indicators:
            if word in narration or word in context:
                score -= 3
        
        # Look for specific patterns
        if re.search(r'UPI-\w+', narration):
            score += 5  # UPI payments are almost always withdrawals
        if re.search(r'(?:salary|sal).*(?:credit|cr)', narration, re.IGNORECASE):
            score -= 10  # Salary credits are definitely deposits
        
        return score

    def parse_pdf(self, pdf_path, is_ai_parser=False, print_raw_text=False):
        """
        Parse a bank statement PDF file
        Supports multiple PDF reader libraries
        """
        # Print raw extracted text for debugging
        if print_raw_text:
            self.print_raw_pdf_text(pdf_path)
        
        # Check if the file exists
        if not os.path.exists(pdf_path):
            print(f"Error: File not found: {pdf_path}")
            return []
        
        pdf_text = ""
        
        try:
            # Extract text based on available library
            if PDF_READER == "pymupdf":
                with fitz.open(pdf_path) as pdf:
                    for page in pdf:
                        pdf_text += page.get_text()
                        
            elif PDF_READER == "pdfplumber":
                with pdfplumber.open(pdf_path) as pdf:
                    for page in pdf.pages:
                        text = page.extract_text()
                        if text:
                            pdf_text += text + "\n"
                            
            elif PDF_READER == "pdf2image+pytesseract":
                # Convert PDF to images and extract text with OCR
                images = convert_from_path(pdf_path)
                for image in images:
                    text = pytesseract.image_to_string(image)
                    pdf_text += text + "\n"
            
            # If AI parser, use transformer-based extraction
            if is_ai_parser:
                return self.parse_with_layout(pdf_text)
            else:
                # Use rule-based parser
                return self.parse_with_regex(pdf_text)
                
        except Exception as e:
            print(f"Error extracting text from PDF: {str(e)}")
            return []
    
    def print_raw_pdf_text(self, pdf_path):
        """
        Print the raw extracted text from a PDF file for debugging
        """
        print(f"\n\n============== RAW EXTRACTED TEXT FROM PDF: {pdf_path} ==============\n")
        
        try:
            # Extract text based on available library
            if PDF_READER == "pymupdf":
                with fitz.open(pdf_path) as pdf:
                    for i, page in enumerate(pdf):
                        text = page.get_text()
                        print(f"\n----- PAGE {i+1} -----\n")
                        print(text)
                        
            elif PDF_READER == "pdfplumber":
                with pdfplumber.open(pdf_path) as pdf:
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
                            
            elif PDF_READER == "pdf2image+pytesseract":
                # Convert PDF to images and extract text with OCR
                images = convert_from_path(pdf_path)
                for i, image in enumerate(images):
                    text = pytesseract.image_to_string(image)
                    print(f"\n----- PAGE {i+1} -----\n")
                    print(text)
            
            # Also save to a text file for easier reference
            output_path = f"{pdf_path}.raw.txt"
            with open(output_path, 'w', encoding='utf-8') as f:
                if PDF_READER == "pymupdf":
                    with fitz.open(pdf_path) as pdf:
                        for i, page in enumerate(pdf):
                            text = page.get_text()
                            f.write(f"\n----- PAGE {i+1} -----\n\n")
                            f.write(text + "\n")
                elif PDF_READER == "pdfplumber":
                    with pdfplumber.open(pdf_path) as pdf:
                        for i, page in enumerate(pdf.pages):
                            text = page.extract_text()
                            f.write(f"\n----- PAGE {i+1} -----\n\n")
                            f.write(text + "\n")
                elif PDF_READER == "pdf2image+pytesseract":
                    images = convert_from_path(pdf_path)
                    for i, image in enumerate(images):
                        text = pytesseract.image_to_string(image)
                        f.write(f"\n----- PAGE {i+1} -----\n\n")
                        f.write(text + "\n")
            
            print(f"\nRaw text also saved to: {output_path}")
            
        except Exception as e:
            print(f"Error extracting text: {str(e)}")
            
        print("\n============== END OF RAW EXTRACTED TEXT ==============\n")

@app.route('/api/ai-parser/parse-pdf', methods=['POST'])
def parse_pdf():
    try:
        # Check if file was uploaded
        if 'file' not in request.files:
            return jsonify({"error": "No file uploaded"}), 400
            
        file = request.files['file']
        debug_mode = request.form.get('debug', 'false').lower() == 'true'
        
        # Check if a valid file was selected
        if file.filename == '':
            return jsonify({"error": "No file selected"}), 400
            
        if file and file.filename.endswith('.pdf'):
            # Save uploaded file temporarily
            temp_dir = tempfile.mkdtemp()
            temp_path = os.path.join(temp_dir, secure_filename(file.filename))
            file.save(temp_path)
            
            parser = BankStatementParser()
            
            # Print raw text if in debug mode
            if debug_mode:
                parser.print_raw_pdf_text(temp_path)
            
            # Parse the PDF with AI approach
            start_time = time.time()
            transactions = parser.parse_with_layout(temp_path)
            end_time = time.time()
            
            print(f"AI Parser extracted {len(transactions)} transactions")
            if transactions:
                print(f"Sample first transaction: {transactions[0]}")
                
            print(f"Time taken: {end_time - start_time:.2f} seconds")
            
            # Clean up
            shutil.rmtree(temp_dir)
            
            return jsonify(transactions)
        else:
            return jsonify({"error": "Invalid file format. Please upload a PDF file"}), 400
    except Exception as e:
        print(f"Error: {str(e)}")
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    import sys
    
    # Check if command-line arguments are provided
    if len(sys.argv) > 1:
        # Handle command-line mode for debugging/testing
        parser = argparse.ArgumentParser(description='Bank Statement Parser')
        parser.add_argument('pdf_path', help='Path to PDF file')
        parser.add_argument('--raw', action='store_true', help='Print raw extracted text')
        parser.add_argument('--ai', action='store_true', help='Use AI-based parser')
        parser.add_argument('--debug', action='store_true', help='Debug mode with extra output')
        
        args = parser.parse_args()
        
        statement_parser = BankStatementParser()
        
        # Print raw text if requested
        if args.raw:
            statement_parser.print_raw_pdf_text(args.pdf_path)
        
        # Parse the PDF if not just raw text mode
        if not args.raw or args.debug:
            if args.ai:
                transactions = statement_parser.parse_with_layout(args.pdf_path)
            else:
                # Extract text and use rule-based parser
                with pdfplumber.open(args.pdf_path) as pdf:
                    text = ''
                    for page in pdf.pages:
                        extracted = page.extract_text()
                        if extracted:
                            text += extracted + '\n'
                
                transactions = statement_parser.parse_with_regex(text)
            
            print(f"\nFound {len(transactions)} transactions:")
            print(json.dumps(transactions, indent=2))
    else:
        # Run as Flask app
        app.run(host='0.0.0.0', port=5000) 