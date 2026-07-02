# Hotel Scraper Runner - PowerShell Version
# Interactive mode: User enters hotel name when prompted

Write-Host "Starting Agoda Hotel Scraper..." -ForegroundColor Green
Write-Host ""

# Change to script directory
Push-Location $PSScriptRoot

# Compile and run
mvn clean compile exec:java -Dexec.mainClass="scraper.AgodaHotelScraper"

# Restore directory
Pop-Location

Write-Host ""
Write-Host "Scraper execution complete." -ForegroundColor Green
