FROM python:3.9-slim

# Install system dependencies
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    libgl1-mesa-glx \
    libglib2.0-0 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy requirements and install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy the application code
COPY bank_statement_parser.py .

# Create upload directory
RUN mkdir -p temp_uploads

# Expose port
EXPOSE 5000

# Command to run the application
CMD ["python", "bank_statement_parser.py"] 