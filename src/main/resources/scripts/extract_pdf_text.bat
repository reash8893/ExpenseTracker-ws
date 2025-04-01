@echo off
REM Batch file to compile and run the ExtractRawPDFText class

REM Change to the project root directory (assuming this script is in src/main/resources/scripts)
cd ..\..\..\..\

REM Ensure the target directory exists
if not exist target\classes mkdir target\classes

REM Find the PDFBox JAR files in the Maven repository
SET PDFBOX_JAR=%USERPROFILE%\.m2\repository\org\apache\pdfbox\pdfbox\2.0.27\pdfbox-2.0.27.jar
SET COMMONS_JAR=%USERPROFILE%\.m2\repository\commons-logging\commons-logging\1.2\commons-logging-1.2.jar
SET FONTBOX_JAR=%USERPROFILE%\.m2\repository\org\apache\pdfbox\fontbox\2.0.27\fontbox-2.0.27.jar

REM If the PDFBox JARs aren't found in the Maven repository, look in the application's target/dependency folder
if not exist "%PDFBOX_JAR%" (
    SET PDFBOX_JAR=target\dependency\pdfbox-2.0.27.jar
    SET COMMONS_JAR=target\dependency\commons-logging-1.2.jar
    SET FONTBOX_JAR=target\dependency\fontbox-2.0.27.jar
)

REM Define the classpath with all required JARs
SET CLASSPATH=.;%PDFBOX_JAR%;%COMMONS_JAR%;%FONTBOX_JAR%

REM Compile the Java file
echo Compiling ExtractRawPDFText.java...
javac -cp "%CLASSPATH%" src\main\resources\scripts\ExtractRawPDFText.java -d target\classes

REM Check if compilation was successful
if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed.
    exit /b 1
)

REM Run the compiled class
echo Running ExtractRawPDFText...
java -cp "%CLASSPATH%;target\classes" ExtractRawPDFText %*

REM If there's an error running the class
if %ERRORLEVEL% NEQ 0 (
    echo Execution failed.
    exit /b 1
)

echo Done. 