@echo off
REM Hotel Scraper Runner - Direct Java execution with full classpath
REM Usage: run_direct.bat "Hotel Name"

if "%~1"=="" (
    echo Usage: run_direct.bat "Hotel Name"
    echo Example: run_direct.bat "Le Meridian"
    exit /b 1
)

cd /d "%~dp0"

REM Build classpath
setlocal enabledelayedexpansion
set CLASSPATH=target\classes
for /r target\dependency %%A in (*.jar) do set CLASSPATH=!CLASSPATH!;%%A
for /r ~/.m2/repository/com/microsoft/playwright %%A in (*.jar) do set CLASSPATH=!CLASSPATH!;%%A

REM Run the scraper
java -cp "!CLASSPATH!" scraper.AgodaHotelScraper "%~1"
