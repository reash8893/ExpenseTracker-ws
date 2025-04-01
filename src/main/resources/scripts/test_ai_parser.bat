@echo off
echo Testing AI Parser API...

REM Try a simple GET request to see if the server is running
curl -X GET http://localhost:5000

echo.
echo If you see HTML output above, the AI Parser server is running.

echo.
echo Note: The actual API endpoint is POST /api/ai-parser/parse-pdf 
echo and requires a PDF file to be uploaded.
echo.
echo To properly test with a PDF file, use Postman or curl with a file:
echo curl -X POST -F "file=@path/to/your.pdf" http://localhost:5000/api/ai-parser/parse-pdf
echo.
echo To fix any issues:
echo 1. Run rebuild_ai_parser.bat to restart the container
echo 2. Check docker logs bank-statement-parser for any errors
echo. 