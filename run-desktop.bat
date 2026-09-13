@echo off
REM Launches the Desktop Sender (screen share + remote control + audio).
REM Start the backend first:  cd backend && npm start

set "JAVA=C:\Program Files\Java\jdk-22\bin\java.exe"
set "JAR=%~dp0desktop\target\desktop-sender-1.0.0-shaded.jar"

if not exist "%JAVA%" (
    echo JDK 22 not found at "%JAVA%"
    pause
    exit /b 1
)
if not exist "%JAR%" (
    echo JAR not found: "%JAR%"
    echo Build it first:  cd desktop ^&^& mvn package
    pause
    exit /b 1
)

echo Starting Desktop Sender...
"%JAVA%" -jar "%JAR%"
pause
