@echo off
echo Stopping existing container...
docker stop bank-statement-parser 2>NUL
docker rm bank-statement-parser 2>NUL

echo Rebuilding AI Parser Docker Image...
cd %~dp0
docker build -t bank-statement-ai-parser .

echo Starting AI Parser Service (CPU mode)...
docker run -d -p 5000:5000 --name bank-statement-parser bank-statement-ai-parser

echo.
echo AI Parser Service has been rebuilt and restarted on http://localhost:5000
echo Use the endpoint http://localhost:9080/api/parser/parse-pdf-ai to process PDFs
echo. 