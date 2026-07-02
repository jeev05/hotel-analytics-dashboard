# Spring Boot API + Frontend Setup Guide

## 🚀 Quick Start

### 1. **Start the Backend API (Port 8080)**

**Windows (Batch):**
```bash
run-api.bat
```

**Windows (PowerShell):**
```powershell
.\run-api.ps1
```

**Any OS (Maven):**
```bash
mvn spring-boot:run
```

The API will start on **http://localhost:8080**

### 2. **Test the API**

**Health Check:**
```bash
curl http://localhost:8080/api/health
```

**Scrape a Hotel:**
```bash
curl "http://localhost:8080/api/scrape?hotelName=Taj%20Hotel%20Mumbai"
```

### 3. **Open Frontend Dashboard**

Once the backend is running, open the frontend:

```bash
# Option 1: Directly open in browser
frontend/index.html

# Option 2: Start a local server (Python)
python -m http.server 8000
# Then visit: http://localhost:8000/frontend/
```

---

## 📋 API Endpoints

### GET /api/scrape?hotelName=XYZ
Scrapes hotel data from Booking.com

**Query Parameters:**
- `hotelName` (required) - Name of the hotel to scrape

**Response:**
```json
{
  "error": null,
  "hotel": {
    "name": "Hotel Name",
    "address": "Address",
    "price": "₹15,000",
    "rating": "4.5",
    "reviews": "1000",
    "amenities": ["WiFi", "Pool"],
    "sourceUrl": "https://..."
  },
  "rooms": [
    {
      "roomType": "Standard Room",
      "guests": "1-2",
      "price": "₹12,000",
      "availability": "Available"
    }
  ],
  "competitorAnalysis": {
    "priceComparison": "...",
    "ratingComparison": "...",
    "strengths": [...],
    "weaknesses": [...],
    "recommendations": [...]
  }
}
```

### GET /api/health
Health check endpoint

**Response:**
```json
{
  "status": "Hotel Scraper API is running"
}
```

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────┐
│         Frontend (HTML/CSS/JS)          │
│  (localhost:8000/frontend/index.html)   │
└──────────────┬──────────────────────────┘
               │ HTTP GET /api/scrape
               ↓
┌──────────────────────────────────────────────┐
│   Spring Boot REST API (Port 8080)           │
│  ┌──────────────────────────────────────┐    │
│  │  HotelController                     │    │
│  │  - /api/scrape                       │    │
│  │  - /api/health                       │    │
│  └──────────────┬───────────────────────┘    │
│                 │ calls                       │
│  ┌──────────────▼───────────────────────┐    │
│  │  HotelScraperService                 │    │
│  │  - scrapeHotel()                     │    │
│  │  - convertToDTO()                    │    │
│  └──────────────┬───────────────────────┘    │
│                 │ uses                        │
│  ┌──────────────▼───────────────────────┐    │
│  │ Playwright Browser                   │    │
│  │ - Navigate to Booking.com            │    │
│  │ - Extract hotel data                 │    │
│  │ - Parse rooms & amenities            │    │
│  └──────────────────────────────────────┘    │
└──────────────────────────────────────────────┘
```

---

## 📦 Project Structure

```
hotel-scrape/
├── pom.xml                          (Maven config with Spring Boot)
├── run-api.bat                      (Windows startup script)
├── run-api.sh                       (Linux/Mac startup script)
├── run-api.ps1                      (PowerShell startup script)
│
├── src/main/
│   ├── java/
│   │   ├── com/hotelapi/
│   │   │   ├── HotelScraperApplication.java    (Spring Boot main class)
│   │   │   ├── controller/
│   │   │   │   └── HotelController.java        (REST endpoints)
│   │   │   ├── service/
│   │   │   │   └── HotelScraperService.java    (Business logic)
│   │   │   └── dto/
│   │   │       ├── HotelResponseDTO.java
│   │   │       ├── HotelDTO.java
│   │   │       ├── RoomOptionDTO.java
│   │   │       └── CompetitorAnalysisDTO.java
│   │   ├── scraper/
│   │   │   ├── HotelScraper.java               (Playwright scraper)
│   │   │   └── CompetitorAnalyzer.java
│   │   └── model/
│   │       ├── Hotel.java
│   │       ├── RoomOption.java
│   │       └── CompetitorAnalysis.java
│   └── resources/
│       └── application.properties               (Spring config)
│
└── frontend/
    ├── index.html                   (Modern dashboard UI)
    ├── style.css                    (Professional styling)
    └── script.js                    (API integration)
```

---

## 🔧 Configuration

### application.properties

```properties
server.port=8080
server.servlet.context-path=/
spring.application.name=Hotel Scraper API
logging.level.root=INFO
logging.level.com.hotelapi=INFO
```

Modify these settings as needed.

---

## 💡 Features

✅ **REST API** - Clean endpoints for hotel scraping  
✅ **CORS Enabled** - Works with localhost frontend  
✅ **Error Handling** - Graceful error responses  
✅ **Headless Browsing** - Fast Playwright-based scraping  
✅ **Data Extraction** - Hotels, rooms, amenities, pricing  
✅ **Competitor Analysis** - Compare with other properties  
✅ **Modern UI** - Professional dashboard with cards and grids  

---

## 🚨 Troubleshooting

### Port 8080 already in use
```bash
# Find process using port 8080 (Windows)
netstat -ano | findstr :8080

# Kill the process
taskkill /PID <PID> /F

# Or use a different port in application.properties
server.port=8081
```

### Frontend can't connect to API
- Ensure backend is running: `curl http://localhost:8080/api/health`
- Check CORS settings in HotelController
- Verify API URL in `frontend/script.js`: `const API_BASE_URL = 'http://localhost:8080/api'`

### Playwright browser issues
- Ensure at least 500MB disk space
- First run downloads Chromium (~300MB)
- Use headless mode for servers (already set in code)

---

## 📝 Notes

- Frontend works alongside the API (no build required)
- Using Spring Boot 3.2 for modern Java 21 support
- All scraping is headless for better performance
- CORS configured for localhost development
