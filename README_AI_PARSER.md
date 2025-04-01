# AI-Powered Bank Statement Parser

This project provides an AI-powered bank statement parser that can extract transactions from PDF bank statements from various banks and financial institutions.

## Overview

Instead of writing custom regex patterns for each bank's statement format, this solution uses AI and computer vision techniques to:

1. Detect and extract tables from bank statements
2. Parse transaction data using OCR and document understanding
3. Structure the extracted data into a consistent format

## Key Features

- **Format Agnostic**: Works with statements from different banks without hardcoded patterns
- **Table Detection**: Uses computer vision to find transaction tables
- **Smart Extraction**: Uses OCR and contextual understanding to extract transaction details
- **Post-processing**: Validates and corrects data (e.g., withdrawal vs deposit classification)
- **Fallback Mechanism**: If AI parsing fails, falls back to rule-based parsing

## Architecture

The solution consists of two components:

1. **Python AI Service**: A microservice that uses computer vision, OCR, and AI to extract data from PDFs
2. **Java Integration**: A Java client that communicates with the Python service

## Setup Instructions

### Prerequisites

- Docker installed on your system
- Java 11 or higher
- Maven

### Starting the AI Parser Service

1. Navigate to the scripts directory:
   ```
   cd src/main/resources/scripts
   ```

2. Start the AI parser service using the provided batch script:
   ```
   start_ai_parser.bat
   ```

This will build and start a Docker container running the AI parser service.

### Using the AI Parser

The application now has two endpoints for parsing bank statements:

1. **Standard Parser** (rule-based): 
   - Endpoint: `http://localhost:9080/api/parser/parse-pdf`
   - This uses the original rule-based parser

2. **AI Parser** (with fallback): 
   - Endpoint: `http://localhost:9080/api/parser/parse-pdf-ai`
   - This tries the AI parser first, then falls back to the rule-based parser if needed

To use the AI parser:

```bash
# Using curl
curl -X POST -F "file=@your_statement.pdf" http://localhost:9080/api/parser/parse-pdf-ai

# Using Postman
# Create a POST request to http://localhost:9080/api/parser/parse-pdf-ai
# Add your PDF file as form data with key "file"
```

## How It Works

1. **Table Detection**: The service uses OpenCV to detect tables in the PDF
2. **OCR Processing**: Tesseract OCR extracts text with position information
3. **Structured Extraction**: The service identifies dates, amounts, and narrations
4. **Post-processing**: Transactions are validated for consistency and accuracy
5. **JSON Response**: Structured data is returned as JSON conforming to the BankTransaction model

## Extending the Solution

### Adding Advanced AI Models

The solution is designed to use more advanced AI models like LayoutLM when available:

1. Download a pre-trained LayoutLM model
2. Place it in the appropriate directory
3. The service will automatically use it if available

### Fine-tuning for Better Results

To improve accuracy for specific banks:

1. Collect a set of sample statements
2. Fine-tune the LayoutLM model on your specific data
3. Replace the generic model with your fine-tuned model

## Troubleshooting

### AI Parser Service Not Available

If the AI parser service isn't responding:

1. Check that the Docker container is running:
   ```
   docker ps
   ```

2. Check logs for errors:
   ```
   docker logs bank-statement-parser
   ```

3. Restart the service:
   ```
   docker restart bank-statement-parser
   ```

### Poor Extraction Results

If the extraction quality is poor:

1. Try increasing the image resolution in the Python service
2. Ensure the PDF is not encrypted or password-protected
3. Check if the PDF contains actual text or just images

## License

This project is licensed under the MIT License - see the LICENSE file for details. 