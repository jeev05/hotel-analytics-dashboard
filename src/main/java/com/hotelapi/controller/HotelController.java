package com.hotelapi.controller;

import com.hotelapi.dto.HotelResponseDTO;
import com.hotelapi.service.HotelScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import model.Hotel;
import model.RoomOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/api")
public class HotelController {

    @Autowired
    private HotelScraperService hotelScraperService;

    /**
     * GET /api/scrape?hotelName=XYZ&source=booking|agoda
     * Scrapes hotel data for the given hotel name and source.
     * 
     * @param hotelName The name of the hotel to scrape
     * @param source    booking or agoda (default: booking)
     * @return HotelResponseDTO with hotel details, rooms, and competitor analysis
     */
    @GetMapping("/scrape")
    public ResponseEntity<HotelResponseDTO> scrapeHotel(
            @RequestParam(name = "hotelName") String hotelName,
            @RequestParam(name = "source", defaultValue = "booking") String source) {
        
        if (hotelName == null || hotelName.trim().isEmpty()) {
            return ResponseEntity
                    .badRequest()
                    .body(new HotelResponseDTO("Hotel name cannot be empty", null, null, null));
        }

        String normalizedSource = source == null ? "booking" : source.trim().toLowerCase();
        if (!"booking".equals(normalizedSource) && !"agoda".equals(normalizedSource)) {
            return ResponseEntity
                    .badRequest()
                    .body(new HotelResponseDTO("Invalid source. Use source=booking or source=agoda", null, null, null));
        }

        try {
            HotelResponseDTO response = hotelScraperService.scrapeHotel(hotelName, normalizedSource);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HotelResponseDTO("Error scraping hotel: " + e.getMessage(), null, null, null));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("{\"status\": \"Hotel Scraper API is running\"}");
    }

    // Helper methods for mock data
    private Hotel createMockHotel(String hotelName) {
        Hotel hotel = new Hotel();
        Random random = new Random();

        // Set hotel name
        hotel.setName(hotelName);

        // Set location based on hotel name
        if (hotelName.toLowerCase().contains("taj mahal palace")) {
            hotel.setAddress("Apollo Bunder, Colaba, Mumbai, Maharashtra 400001, India");
            hotel.setPrice("$" + (400 + random.nextInt(200)));
            hotel.setRating("4." + (7 + random.nextInt(3)));
            hotel.setReviews(String.valueOf(800 + random.nextInt(400)));
            hotel.setDescription("Luxury heritage hotel with colonial architecture and modern amenities.");
            hotel.setAmenities(Arrays.asList("Spa", "Pool", "Fine Dining", "Business Center", "Valet Parking"));
        } else if (hotelName.toLowerCase().contains("itc grand chola")) {
            hotel.setAddress("ITCWindsor, 63, Mount Road, Guindy, Chennai, Tamil Nadu 600032, India");
            hotel.setPrice("$" + (300 + random.nextInt(150)));
            hotel.setRating("4." + (8 + random.nextInt(2)));
            hotel.setReviews(String.valueOf(600 + random.nextInt(300)));
            hotel.setDescription("Modern luxury hotel inspired by Chola dynasty architecture.");
            hotel.setAmenities(Arrays.asList("Spa", "Multiple Pools", "Shopping Mall", "Fitness Center", "Concierge"));
        } else if (hotelName.toLowerCase().contains("leela palace")) {
            hotel.setAddress("Diplomatic Enclave, Chanakyapuri, New Delhi, Delhi 110021, India");
            hotel.setPrice("$" + (350 + random.nextInt(150)));
            hotel.setRating("4." + (6 + random.nextInt(4)));
            hotel.setReviews(String.valueOf(500 + random.nextInt(300)));
            hotel.setDescription("Palatial luxury hotel with presidential suites and extensive gardens.");
            hotel.setAmenities(Arrays.asList("Spa", "Pool", "Tennis Courts", "Business Center", "24/7 Butler Service"));
        } else if (hotelName.toLowerCase().contains("oberoi")) {
            hotel.setAddress("Nariman Point, Mumbai, Maharashtra 400021, India");
            hotel.setPrice("$" + (250 + random.nextInt(150)));
            hotel.setRating("4." + (7 + random.nextInt(3)));
            hotel.setReviews(String.valueOf(400 + random.nextInt(200)));
            hotel.setDescription("Iconic luxury hotel known for exceptional service and elegance.");
            hotel.setAmenities(Arrays.asList("Spa", "Pool", "Fine Dining", "Concierge", "Valet Parking"));
        } else if (hotelName.toLowerCase().contains("park plaza")) {
            hotel.setAddress("42-44 Gloucester Road, Kensington, London SW7 4QL, United Kingdom");
            hotel.setPrice("£" + (150 + random.nextInt(100)));
            hotel.setRating("4." + (2 + random.nextInt(6)));
            hotel.setReviews(String.valueOf(300 + random.nextInt(200)));
            hotel.setDescription("Modern business hotel in the heart of Kensington.");
            hotel.setAmenities(Arrays.asList("Business Center", "Fitness Center", "Bar", "Restaurant", "WiFi"));
        } else if (hotelName.toLowerCase().contains("marriott")) {
            hotel.setAddress("Various locations worldwide");
            hotel.setPrice("$" + (150 + random.nextInt(100)));
            hotel.setRating("4." + (3 + random.nextInt(5)));
            hotel.setReviews(String.valueOf(200 + random.nextInt(300)));
            hotel.setDescription("International hotel chain with consistent quality and service.");
            hotel.setAmenities(Arrays.asList("Pool", "Fitness Center", "Business Center", "Restaurant", "Bar"));
        } else if (hotelName.toLowerCase().contains("hilton")) {
            hotel.setAddress("Various locations worldwide");
            hotel.setPrice("$" + (120 + random.nextInt(80)));
            hotel.setRating("4." + (1 + random.nextInt(7)));
            hotel.setReviews(String.valueOf(250 + random.nextInt(250)));
            hotel.setDescription("Global hospitality leader with diverse hotel portfolio.");
            hotel.setAmenities(Arrays.asList("Pool", "Fitness Center", "Business Center", "Multiple Restaurants", "Lounge"));
        } else if (hotelName.toLowerCase().contains("hyatt")) {
            hotel.setAddress("Various locations worldwide");
            hotel.setPrice("$" + (180 + random.nextInt(120)));
            hotel.setRating("4." + (4 + random.nextInt(4)));
            hotel.setReviews(String.valueOf(180 + random.nextInt(220)));
            hotel.setDescription("Premium hotel brand known for innovation and comfort.");
            hotel.setAmenities(Arrays.asList("Spa", "Pool", "Fitness Center", "Restaurant", "Lounge"));
        } else {
            // Default mock data
            hotel.setAddress("123 " + hotelName + " Street, City, Country");
            hotel.setPrice("$" + (100 + random.nextInt(200)));
            hotel.setRating("4." + random.nextInt(10));
            hotel.setReviews(String.valueOf(50 + random.nextInt(200)));
            hotel.setDescription("A beautiful hotel offering comfortable accommodations and excellent service.");
            hotel.setAmenities(Arrays.asList("WiFi", "Pool", "Restaurant", "Fitness Center", "Room Service"));
        }

        // Set image analysis data
        hotel.setImageScore(75.0 + random.nextInt(25)); // 75-100
        hotel.setBlurry(random.nextBoolean());

        return hotel;
    }

    private List<RoomOption> createMockRooms() {
        List<RoomOption> rooms = new ArrayList<>();
        Random random = new Random();

        String[] roomTypes = {"Standard Room", "Deluxe Room", "Executive Suite", "Presidential Suite"};
        String[] guestOptions = {"1", "2", "2", "4"};

        for (int i = 0; i < 3; i++) {
            RoomOption room = new RoomOption();
            room.setRoomType(roomTypes[i]);
            room.setGuests(guestOptions[i]);
            room.setPrice("$" + (100 + i * 50 + random.nextInt(50)));
            room.setAvailability("Available");
            room.setRoomSize((20 + i * 10 + random.nextInt(20)) + " m²");
            room.setBedInfo("King Bed");
            room.getHighlights().addAll(Arrays.asList("City View", "Free WiFi", "Air Conditioning"));
            room.getAmenities().addAll(Arrays.asList("TV", "Minibar", "Safe", "Coffee Maker"));
            room.getPolicies().add("Free cancellation until 24 hours before check-in");

            rooms.add(room);
        }

        return rooms;
    }
}
