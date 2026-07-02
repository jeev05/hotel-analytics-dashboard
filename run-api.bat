@echo off
REM Hotel Scraper REST API Startup Script
REM Stable mode: package + run jar on port 9090

echo.
echo ========================================
echo Hotel Scraper Spring Boot REST API
echo ========================================
echo.
echo Building application jar...
echo Starting application on port 9090...
echo API will be available at: http://localhost:9090/api
echo Frontend will be available at: http://localhost:9090/
echo.
echo Endpoints:
echo - GET /api/scrape?hotelName=XYZ
echo - GET /api/health
echo.
echo Press Ctrl+C to stop the server.
echo.

cd /d "%~dp0"

mvn -DskipTests package
if errorlevel 1 (
	echo Build failed.
	pause
	exit /b 1
)

java -jar target\hotel-scrape-1.0.jar --server.port=9090

pause
