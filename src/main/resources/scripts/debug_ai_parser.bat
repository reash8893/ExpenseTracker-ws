@echo off
echo Showing real-time logs for AI Parser container...
echo Press Ctrl+C to exit the logs view.
echo.

docker logs -f bank-statement-parser

echo.
echo If you need to restart the container, run rebuild_ai_parser.bat
echo. 