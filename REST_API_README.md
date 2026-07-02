# Hotel Scraper Spring Boot REST API - Quick Reference

## 🎯 What Was Added

Your hotel scraper is now a **production-ready REST API** with Spring Boot!

### New Components Created:
1. **HotelScraperApplication.java** - Spring Boot main application
2. **HotelController.java** - REST endpoints (/api/scrape, /api/health)
3. **HotelScraperService.java** - Business logic layer
4. **DTOs** - JSON response models (HotelDTO, RoomOptionDTO, CompetitorAnalysisDTO)
5. **application.properties** - Configuration file
6. **Startup scripts** - run-api.bat, run-api.sh, run-api.ps1

---

## 🚀 How to Run

### Method 1: Windows Batch (Easiest)
```cmd
run-api.bat
```

### Method 2: PowerShell
```powershell
.\run-api.ps1
```

### Method 3: Direct Maven
```bash
mvn spring-boot:run
```

### Method 4: Package & Run JAR
```bash
mvn clean package
java -jar target/hotel-scrape-1.0.jar
```

---

## 🧪 Test the API

### Health Check
```bash
curl http://localhost:8080/api/health
```

Response:
```json
{"status": "Hotel Scraper API is running"}
```

### Scrape a Hotel
```bash
curl "http://localhost:8080/api/scrape?hotelName=Taj%20Hotel%20Mumbai"
```

Response:
```json
{
  "error": null,
  "hotel": {
    "name": "Taj Hotel Mumbai",
    "address": "Marine Drive, Mumbai",
    "price": "₹15,000",
    "rating": "4.5",
    "reviews": "2847",
    "amenities": ["WiFi", "Pool", "Gym", ...],
    "sourceUrl": "https://booking.com/..."
  },
  "rooms": [
    {"roomType": "Standard", "price": "₹12,000", "guests": "1-2", ...},
    {"roomType": "Deluxe", "price": "₹15,000", "guests": "2-3", ...}
  ],
  "competitorAnalysis": {
    "strengths": [...],
    "weaknesses": [...],
    "recommendations": [...]
  }
}
```

---

## 🔌 Frontend Integration

The frontend is **already configured** to use this API!

Just open `frontend/index.html` in your browser and enter a hotel name. The dashboard will fetch data from the API running on port 8080.

---

## 📊 API Specification

### Endpoint: GET /api/scrape

**URL:** `http://localhost:8080/api/scrape?hotelName=XYZ`

**Parameters:**
| Name | Type | Required | Example |
|------|------|----------|---------|
| hotelName | String | Yes | `Taj Hotel Mumbai` |

**Response Status Codes:**
| Code | Meaning |
|------|---------|
| 200 | Success - Hotel data returned |
| 400 | Bad Request - Hotel name missing |
| 404 | Not Found - No hotels matched the search |
| 500 | Server Error - Scraping failed |

**Response Format:**
```json
{
  "error": "null or error message",
  "hotel": { /* HotelDTO */ },
  "rooms": [ /* Array of RoomOptionDTO */ ],
  "competitorAnalysis": { /* CompetitorAnalysisDTO */ }
}
```

### Endpoint: GET /api/health

**URL:** `http://localhost:8080/api/health`

**Response:**
```json
{"status": "Hotel Scraper API is running"}
```

---

## 🏗️ File Structure - New API Files

```
src/main/java/
├── com/hotelapi/
│   ├── HotelScraperApplication.java          ← Main Spring Boot class
│   ├── controller/
│   │   └── HotelController.java              ← REST endpoints
│   ├── service/
│   │   └── HotelScraperService.java          ← Business logic
│   └── dto/
│       ├── HotelResponseDTO.java             ← Response wrapper
│       ├── HotelDTO.java                     ← Hotel data
│       ├── RoomOptionDTO.java                ← Room data
│       └── CompetitorAnalysisDTO.java        ← Analysis data
│
src/main/resources/
└── application.properties                    ← Spring config

run-api.bat                                   ← Windows launcher
run-api.sh                                    ← Linux/Mac launcher
run-api.ps1                                   ← PowerShell launcher
```

---

## 🎨 Architecture Overview

```
┌──────────────────────────────────────────┐
│   Frontend Dashboard (HTML/CSS/JS)       │
│   http://localhost:3000/frontend/        │
└────────────────┬─────────────────────────┘
                 │ HTTP Request
                 │ GET /api/scrape?hotelName=XYZ
                 ↓
┌──────────────────────────────────────────┐
│   Spring Boot REST API (Port 8080)       │
│   ┌────────────────────────────────────┐ │
│   │ @RestController (HotelController)  │ │
│   │ - /api/scrape                      │ │
│   │ - /api/health                      │ │
│   └────────┬─────────────────────────┬─┘ │
│            │ calls                   │    │
│   ┌────────▼──────────────────────┐  │    │
│   │ @Service (HotelScraperService) │  │    │
│   │ - scrapeHotel()                │  │    │
│   │ - extractHotelDetails()        │  │    │
│   │ - convertToDTO()               │  │    │
│   └────────┬──────────────────────┬┘  │    │
│            │ uses                  │   │    │
│   ┌────────▼─────────────────────┐│   │    │
│   │ Playwright Browser           ││   │    │
│   │ - Navigate                   ││   │    │
│   │ - Scrape Data                ││   │    │
│   │ - Extract Info               ││   │    │
│   └──────────────────────────────┘│   │    │
│                                    └────    │
│  ↓ returns JSON response (HotelResponseDTO)│
└──────────────────────────────────────────┘
                 │
                 │ JSON Response
                 ↓
    Dashboard displays data
    (Hotel Info, Rooms, Analysis)
```

---

## 🔒 CORS Configuration

CORS is **automatically enabled** for local development:
- ✅ `http://localhost:3000`
- ✅ `http://localhost:8000`
- ✅ `file://` (local HTML files)

To add more origins, edit `HotelScraperApplication.java`:

```java
registry.addMapping("/api/**")
    .allowedOrigins(
        "http://localhost:3000",
        "http://localhost:8000",
        "http://example.com"  // Add your domain
    )
```

---

## ⚙️ Configuration

### Change Port Number

Edit `src/main/resources/application.properties`:

```properties
server.port=8080        # Change to 9000, 5000, etc.
```

### Change Logging Level

```properties
logging.level.com.hotelapi=DEBUG    # More detailed logs
logging.level.root=WARN             # Less noise
```

### Disable Headless Mode (Show Browser)

Edit `HotelScraperService.java` line 45:
```java
.setHeadless(false)  // Shows browser while scraping
```

---

## 🐛 Common Issues & Solutions

### Issue: "Port 8080 is already in use"
**Solution:** Kill the process or use different port:
```cmd
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Or change port in application.properties
server.port=8081
```

### Issue: "Playwright browsers not found"
**Solution:** First run takes time to download Chromium (~300MB). Wait and retry.

### Issue: "Frontend shows 'API Error'"
**Solution:** Check:
1. Backend is running: `curl http://localhost:8080/api/health`
2. API URL is correct in `frontend/script.js`
3. Browser console for CORS errors

### Issue: Slow scraping
**Solution:** Headless mode is already enabled. For faster results:
- Increase timeouts in application.properties
- Use dedicated hardware
- Implement caching

---

## 📚 Next Steps

1. **Deploy to Cloud** - Use Docker + deploy to AWS/Azure
2. **Add Database** - Store hotel data in PostgreSQL/MongoDB
3. **Add Authentication** - Secure API with JWT tokens
4. **Add Caching** - Redis for faster responses
5. **Add Scheduling** - Regular scraping with Quartz

---

## 📖 Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Framework | Spring Boot | 3.2.0 |
| Java | OpenJDK | 21 |
| Browser Automation | Playwright | 1.44.0 |
| Build Tool | Maven | 3.x |
| Frontend | Vanilla JS + HTML/CSS | Latest |

---

## ✅ Checklist

- [x] Spring Boot REST API created
- [x] Hotel scraping service implemented
- [x] CORS configured for frontend
- [x] Error handling added
- [x] DTO models created
- [x] API documentation written
- [x] Startup scripts created
- [x] Frontend dashboard already works with API

**You're ready to go! 🚀**
