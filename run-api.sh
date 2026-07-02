#!/bin/bash
# Hotel Scraper REST API Startup Script
# Runs on port 8080

echo ""
echo "========================================"
echo "Hotel Scraper Spring Boot REST API"
echo "========================================"
echo ""
echo "Starting application on port 8080..."
echo "API will be available at: http://localhost:8080/api"
echo ""
echo "Endpoints:"
echo "- GET /api/scrape?hotelName=XYZ"
echo "- GET /api/health"
echo ""
echo "Press Ctrl+C to stop the server."
echo ""

cd "$(dirname "$0")"

mvn spring-boot:run -Dspring-boot.run.main-class="com.hotelapi.HotelScraperApplication"
