# Hotel Scraper Spring Boot REST API - PowerShell Startup

Write-Host "`n========================================" -ForegroundColor Green
Write-Host "Hotel Scraper Spring Boot REST API" -ForegroundColor Green
Write-Host "========================================`n" -ForegroundColor Green

Write-Host "Starting application on port 9090..." -ForegroundColor Cyan
Write-Host "API will be available at: http://localhost:9090/api" -ForegroundColor Cyan
Write-Host "Frontend will be available at: http://localhost:9090/`n" -ForegroundColor Cyan

Write-Host "Endpoints:" -ForegroundColor Yellow
Write-Host "- GET /api/scrape?hotelName=XYZ" -ForegroundColor Yellow
Write-Host "- GET /api/health`n" -ForegroundColor Yellow

Write-Host "Press Ctrl+C to stop the server.`n" -ForegroundColor Magenta

Push-Location $PSScriptRoot

mvn spring-boot:run -Dspring-boot.run.main-class="com.hotelapi.HotelScraperApplication"

Pop-Location
