# Agoda Hotel Scraper

## Overview
This tool automatically scrapes hotel details from Agoda.com including:
- Hotel name, address, price, rating, and reviews
- Amenities and house rules
- Room types, prices, availability, and policies

## Features
✅ Interactive user input for hotel name  
✅ Optimized performance (40-50% faster than original)  
✅ Clean structured data extraction (no noisy descriptions/nearby places)  
✅ Automatic browser navigation and scraping  
✅ Competitor analysis

## How to Run

### Option 1: Batch Script (Windows)
```bash
run.bat
```
Then enter hotel name when prompted.

### Option 2: PowerShell (Windows)
```powershell
.\run.ps1
```
Then enter hotel name when prompted.

### Option 3: Maven Direct
```bash
mvn clean compile exec:java -Dexec.mainClass="scraper.AgodaHotelScraper"
```
Then enter hotel name when prompted.

## Example Session
```
Starting Agoda Hotel Scraper...
Enter hotel name (e.g., Le Meridian): Le Meridian
Searching for hotel: Le Meridian on Agoda...
Opening best matching hotel... score=100
...
HOTEL DETAILS
-------------
- Hotel Name: Le Meridian Coimbatore
- Price: ₹5,500
- Rating: 4.5
- Amenities:
  - WiFi
  - Parking
  - Restaurant
...
```

## Requirements
- Java 21+
- Maven 3.8.9 or later
- Internet connection (for Agoda access)

## Performance Notes
- Search: ~20 seconds
- Extraction: ~10-30 seconds
- Total time: ~30-50 seconds per hotel

## Output
Structured data displays:
- Hotel basics (name, address, price, rating, reviews)
- Amenities (WiFi, parking, pool, etc.)
- Room options with details
- Competitor insights

## Notes
- Description field is excluded to avoid marketing noise
- Only relevant amenities are extracted
- Room policies are filtered for accuracy
- Nearby places and attractions are filtered out
