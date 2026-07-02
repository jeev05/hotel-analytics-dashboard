@echo off
REM Hotel Scraper Runner - Interactive Mode
REM User will be prompted to enter hotel name

cd /d "%~dp0"

echo Starting Agoda Hotel Scraper...
echo.

REM Compile and run with Maven exec
mvn clean compile exec:java -Dexec.mainClass="scraper.AgodaHotelScraper"

pause

