@echo off
echo Starting AI Parser Service...

REM Check if container exists but is stopped
docker start bank-statement-parser >NUL 2>&1
if %ERRORLEVEL% EQU 0 (
    echo AI Parser Service started successfully on http://localhost:5000
) else (
    echo Container does not exist. Please run rebuild_ai_parser.bat first.
    echo Or the container might already be running.
)

echo.
echo Use the endpoint http://localhost:9080/api/parser/parse-pdf-ai to process PDFs
echo. 