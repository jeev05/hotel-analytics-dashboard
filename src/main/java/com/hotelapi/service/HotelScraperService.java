package com.hotelapi.service;

import com.hotelapi.dto.*;
import com.hotelapi.dto.BookingAgodaComparisonDTO;
import model.Hotel;
import model.RoomOption;
import scraper.AgodaScraper;
import scraper.CompetitorAnalyzer;
import scraper.HotelScraper;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HotelScraperService {
    
    private static final Logger LOGGER = Logger.getLogger(HotelScraperService.class.getName());
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)");

    /**
     * Scrapes hotel data and returns it as a structured response
     * 
     * @param hotelName The name of the hotel to scrape
     * @return HotelResponseDTO with complete hotel information
     */
    public HotelResponseDTO scrapeHotel(String hotelName) {
        return scrapeHotel(hotelName, "booking");
    }

    public HotelResponseDTO scrapeHotel(String hotelName, String source) {
        try {
            String normalizedSource = source == null ? "booking" : source.trim().toLowerCase();
            LOGGER.log(Level.INFO, "Starting scrape for hotel: {0} (source: {1})", new Object[]{hotelName, normalizedSource});

            Hotel bookingHotel = scrapeHotelData(hotelName, "booking");
            Hotel agodaHotel = scrapeHotelData(hotelName, "agoda");
            Hotel selectedHotel = "agoda".equals(normalizedSource) ? agodaHotel : bookingHotel;

            if (selectedHotel == null) {
                LOGGER.log(Level.WARNING, "Could not scrape hotel: {0} (source: {1})", new Object[]{hotelName, normalizedSource});
                return new HotelResponseDTO("Hotel not found", null, null, null);
            }

            HotelDTO hotelDTO = convertHotelToDTO(selectedHotel, normalizedSource);
            List<RoomOptionDTO> roomsDTO = convertRoomsToDTO(selectedHotel.getRoomOptions());
            CompetitorAnalysisDTO analysisDTO = convertAnalysisToDTO(selectedHotel);
            BookingAgodaComparisonDTO comparisonDTO = buildBookingAgodaComparison(bookingHotel, agodaHotel);

            LOGGER.log(Level.INFO, "Returning API response for hotel: {0} (source: {1}, rooms: {2}, competitorAnalysis entries: {3})",
                    new Object[]{hotelName, normalizedSource, roomsDTO.size(),
                            analysisDTO == null || analysisDTO.getPriceComparison() == null ? 0 : analysisDTO.getPriceComparison().size()});
            LOGGER.log(Level.INFO, "Successfully scraped hotel: {0} (source: {1})", new Object[]{hotelName, normalizedSource});

            HotelResponseDTO response = new HotelResponseDTO(null, hotelDTO, roomsDTO, analysisDTO);
            response.setComparison(comparisonDTO);

            LOGGER.info("BOOKING HOTEL FOUND: " + (bookingHotel != null));
            LOGGER.info("AGODA HOTEL FOUND: " + (agodaHotel != null));
            LOGGER.info("COMPARISON CREATED");
            LOGGER.info("BOOKING AMENITIES = " + (bookingHotel == null ? 0 : (bookingHotel.getAmenities() == null ? 0 : bookingHotel.getAmenities().size())));
            LOGGER.info("AGODA AMENITIES = " + (agodaHotel == null ? 0 : (agodaHotel.getAmenities() == null ? 0 : agodaHotel.getAmenities().size())));
            LOGGER.info("COMMON AMENITIES = " + comparisonDTO.getCommonAmenities());
            LOGGER.info("BOOKING ONLY = " + comparisonDTO.getBookingOnlyAmenities());
            LOGGER.info("AGODA ONLY = " + comparisonDTO.getAgodaOnlyAmenities());
            LOGGER.info("COMPARISON SENT TO FRONTEND");
            return response;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during scraping: " + e.getMessage(), e);
            return new HotelResponseDTO("Error: " + e.getMessage(), null, null, null);
        }
    }

    /**
     * Internal method to scrape hotel data using existing scraper logic
     */
    private Hotel scrapeHotelData(String hotelName, String source) {
        try {
            if ("agoda".equals(source)) {
                return AgodaScraper.scrapeHotelByName(hotelName);
            }
            return HotelScraper.scrapeHotelByName(hotelName);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error scraping hotel data: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Converts Hotel model to HotelDTO for API response
     */
    public HotelDTO convertHotelToDTO(Hotel hotel, String source) {
        HotelDTO dto = new HotelDTO();
        boolean isAgoda = "agoda".equals(source);

        dto.setName(cleanValue(hotel.getName()));
        dto.setAddress(cleanValue(hotel.getAddress()));
        dto.setPrice(cleanValue(hotel.getPrice()));
        dto.setRating(cleanNumberText(hotel.getRating()));

        String cleanedReviews = cleanReviewCount(hotel.getReviews());
        dto.setReviews(cleanedReviews);
        dto.setReviewCount(cleanedReviews);

        dto.setDescription(isAgoda ? cleanAgodaDescription(hotel.getDescription()) : cleanValue(hotel.getDescription()));
        dto.setAmenities(cleanStringList(hotel.getAmenities()));
        dto.setPolicies(cleanStringList(hotel.getHouseRules()));
        dto.setHouseRules(cleanStringList(hotel.getHouseRules()));
        dto.setRoomFeatures(cleanStringList(hotel.getRoomFeatures()));
        dto.setSourceUrl(cleanValue(hotel.getSourceUrl()));
        dto.setImageUrl(cleanValue(hotel.getImageUrl()));
        dto.setLocationScore(null);
        dto.setImageScore(hotel.getImageScore());
        dto.setBlurry(hotel.isBlurry());
        return dto;
    }

    private String cleanValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String cleanNumberText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        Matcher matcher = NUMBER_PATTERN.matcher(normalized);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return normalized.isEmpty() ? null : normalized;
    }

    private String cleanReviewCount(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("[^0-9]", "").trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String cleanAgodaDescription(String description) {
        String cleaned = cleanValue(description);
        if (cleaned == null) {
            return null;
        }

        if (cleaned.length() > 300) {
            int cutoff = cleaned.indexOf(". ", 200);
            if (cutoff > 0) {
                cleaned = cleaned.substring(0, cutoff + 1).trim();
            } else {
                cleaned = cleaned.substring(0, 300).trim();
            }
        }

        if (cleaned.matches("(?i).*\\b(show more|see more|top things to do|property policies|amenities and facilities|more about)\\b.*")) {
            return null;
        }

        return cleaned;
    }

    private BookingVsAgodaComparisonDTO buildBookingVsAgodaComparison(Hotel bookingHotel, Hotel agodaHotel) {
        BookingVsAgodaComparisonDTO dto = new BookingVsAgodaComparisonDTO();

        double bookingRating = bookingHotel == null ? Double.NaN : parseRating(bookingHotel.getRating());
        double agodaRating = agodaHotel == null ? Double.NaN : parseRating(agodaHotel.getRating());
        Set<String> bookingAmenities = bookingHotel == null ? Set.of() : normalizeAmenityNames(bookingHotel.getAmenities());
        Set<String> agodaAmenities = agodaHotel == null ? Set.of() : normalizeAmenityNames(agodaHotel.getAmenities());

        dto.setBookingRating(Double.isNaN(bookingRating) ? null : bookingRating);
        dto.setAgodaRating(Double.isNaN(agodaRating) ? null : agodaRating);
        dto.setBookingAmenitiesCount(bookingAmenities.size());
        dto.setAgodaAmenitiesCount(agodaAmenities.size());

        Set<String> commonAmenitySet = new LinkedHashSet<>(bookingAmenities);
        commonAmenitySet.retainAll(agodaAmenities);

        Set<String> bookingOnlySet = new LinkedHashSet<>(bookingAmenities);
        bookingOnlySet.removeAll(agodaAmenities);

        Set<String> agodaOnlySet = new LinkedHashSet<>(agodaAmenities);
        agodaOnlySet.removeAll(bookingAmenities);

        dto.setCommonAmenities(new ArrayList<>(commonAmenitySet));
        dto.setBookingOnlyAmenities(new ArrayList<>(bookingOnlySet));
        dto.setAgodaOnlyAmenities(new ArrayList<>(agodaOnlySet));

        dto.setRatingWinner(determineRatingWinner(bookingRating, agodaRating));
        dto.setAmenitiesWinner(determineAmenitiesWinner(bookingAmenities.size(), agodaAmenities.size()));
        dto.setOverallRecommendation(buildOverallRecommendation(dto));

        return dto;
    }

    private BookingAgodaComparisonDTO buildBookingAgodaComparison(Hotel bookingHotel, Hotel agodaHotel) {
        BookingAgodaComparisonDTO dto = new BookingAgodaComparisonDTO();

        // Prices as raw strings
        dto.setBookingPrice(bookingHotel == null ? null : cleanValue(bookingHotel.getPrice()));
        dto.setAgodaPrice(agodaHotel == null ? null : cleanValue(agodaHotel.getPrice()));

        // Ratings as strings
        Double bRating = bookingHotel == null ? null : Double.valueOf(parseRating(bookingHotel.getRating()));
        Double aRating = agodaHotel == null ? null : Double.valueOf(parseRating(agodaHotel.getRating()));
        dto.setBookingRating(bRating == null || bRating.isNaN() ? null : String.format("%.1f", bRating));
        dto.setAgodaRating(aRating == null || aRating.isNaN() ? null : String.format("%.1f", aRating));

        // Review counts as digit-only strings (so the frontend can format/compare them)
        dto.setBookingReviewCount(bookingHotel == null ? null : cleanReviewCount(bookingHotel.getReviews()));
        dto.setAgodaReviewCount(agodaHotel == null ? null : cleanReviewCount(agodaHotel.getReviews()));

        Set<String> bookingAmenities = bookingHotel == null ? Set.of() : normalizeAmenityNames(bookingHotel.getAmenities());
        Set<String> agodaAmenities = agodaHotel == null ? Set.of() : normalizeAmenityNames(agodaHotel.getAmenities());

        dto.setBookingAmenitiesCount(bookingAmenities.size());
        dto.setAgodaAmenitiesCount(agodaAmenities.size());

        Set<String> common = new LinkedHashSet<>(bookingAmenities);
        common.retainAll(agodaAmenities);

        Set<String> bookingOnly = new LinkedHashSet<>(bookingAmenities);
        bookingOnly.removeAll(agodaAmenities);

        Set<String> agodaOnly = new LinkedHashSet<>(agodaAmenities);
        agodaOnly.removeAll(bookingAmenities);

        dto.setCommonAmenities(new ArrayList<>(common));
        dto.setBookingOnlyAmenities(new ArrayList<>(bookingOnly));
        dto.setAgodaOnlyAmenities(new ArrayList<>(agodaOnly));

        // Determine winners
        dto.setRatingWinner(determineRatingWinner(bRating == null ? Double.NaN : bRating, aRating == null ? Double.NaN : aRating));
        dto.setAmenitiesWinner(determineAmenitiesWinner(bookingAmenities.size(), agodaAmenities.size()));

        // Improvement suggestions
        List<String> suggestions = new ArrayList<>();
        // If agoda missing amenities that booking has, suggest adding key amenities
        List<String> keyAmenities = List.of("Pool", "Gym", "Airport Shuttle", "Spa");
        for (String key : keyAmenities) {
            if (bookingOnly.contains(key)) {
                suggestions.add("Consider adding " + key);
            }
        }

        // If agoda rating is lower by 0.5+
        if (bRating != null && aRating != null && !bRating.isNaN() && !aRating.isNaN() && (bRating - aRating) >= 0.5) {
            suggestions.add("Improve guest satisfaction");
            suggestions.add("Improve cleanliness");
            suggestions.add("Improve service quality");
        }

        dto.setImprovementSuggestions(suggestions);

        // Overall recommendation
        StringBuilder overall = new StringBuilder();
        boolean bookingStronger = false;
        if ("Booking.com".equals(dto.getRatingWinner())) {
            overall.append("Booking.com has the stronger guest rating. ");
            bookingStronger = true;
        } else if ("Agoda.com".equals(dto.getRatingWinner())) {
            overall.append("Agoda.com has the stronger guest rating. ");
        } else {
            overall.append("Ratings are comparable. ");
        }

        if ("Booking.com".equals(dto.getAmenitiesWinner())) {
            overall.append("Booking.com offers more amenities. ");
            bookingStronger = true;
        } else if ("Agoda.com".equals(dto.getAmenitiesWinner())) {
            overall.append("Agoda.com offers more amenities. ");
        } else {
            overall.append("Amenities counts are similar. ");
        }

        if (!bookingOnly.isEmpty()) {
            overall.append("Priority improvements: ");
            overall.append(String.join(", ", bookingOnly));
            overall.append(". ");
        }

        if (bookingStronger) {
            overall.append("Overall, Booking.com listing appears stronger.");
        } else {
            overall.append("Overall, Agoda.com listing appears stronger or comparable.");
        }

        dto.setOverallRecommendation(overall.toString().trim());

        return dto;
    }

    private Set<String> normalizeAmenityNames(List<String> amenities) {
        if (amenities == null) {
            return new LinkedHashSet<>();
        }

        Set<String> normalized = new LinkedHashSet<>();
        for (String amenity : amenities) {
            if (amenity == null || amenity.isBlank()) {
                continue;
            }
            String lower = amenity.trim().toLowerCase(Locale.ROOT);
            
            // Normalize WiFi variants
            if (lower.contains("free wifi") || lower.contains("wifi") || lower.contains("wireless internet")) {
                normalized.add("WiFi");
            // Normalize Parking
            } else if (lower.contains("free parking") || lower.contains("parking")) {
                normalized.add("Parking");
            // Normalize Family Rooms
            } else if (lower.contains("family room")) {
                normalized.add("Family Rooms");
            // Normalize Room Service
            } else if (lower.contains("room service")) {
                normalized.add("Room Service");
            // Normalize Airport Shuttle
            } else if (lower.contains("airport shuttle") || lower.contains("shuttle")) {
                normalized.add("Airport Shuttle");
            // Normalize Spa
            } else if (lower.contains("spa and wellness") || lower.contains("spa")) {
                normalized.add("Spa");
            // Normalize Non-Smoking Rooms
            } else if (lower.contains("non-smoking room") || lower.contains("non smoking room")) {
                normalized.add("Non-Smoking Rooms");
            // Normalize Gym variants
            } else if (lower.contains("fitness centre") || lower.contains("fitness center") || lower.equals("gym")) {
                normalized.add("Gym");
            // Restaurant
            } else if (lower.contains("restaurant") || lower.contains("dining")) {
                normalized.add("Restaurant");
            // Pool
            } else if (lower.contains("pool") || lower.contains("swimming")) {
                normalized.add("Pool");
            // Business Center
            } else if (lower.contains("business center") || lower.contains("business centre")) {
                normalized.add("Business Center");
            // Concierge
            } else if (lower.contains("concierge")) {
                normalized.add("Concierge");
            // Bar
            } else if (lower.contains("bar")) {
                normalized.add("Bar");
            // Valet Parking
            } else if (lower.contains("valet")) {
                normalized.add("Valet Parking");
            } else {
                // For unrecognized amenities, keep as-is (trimmed)
                normalized.add(amenity.trim());
            }
        }

        return normalized;
    }

    private String determineRatingWinner(double bookingRating, double agodaRating) {
        if (Double.isNaN(bookingRating) && Double.isNaN(agodaRating)) {
            return "Unknown";
        }
        if (Double.isNaN(bookingRating)) {
            return "Agoda.com";
        }
        if (Double.isNaN(agodaRating)) {
            return "Booking.com";
        }
        if (Math.abs(bookingRating - agodaRating) <= 0.3) {
            return "Tie";
        }
        return bookingRating > agodaRating ? "Booking.com" : "Agoda.com";
    }

    private String determineAmenitiesWinner(int bookingAmenitiesCount, int agodaAmenitiesCount) {
        if (bookingAmenitiesCount == agodaAmenitiesCount) {
            return "Tie";
        }
        return bookingAmenitiesCount > agodaAmenitiesCount ? "Booking.com" : "Agoda.com";
    }

    private String buildOverallRecommendation(BookingVsAgodaComparisonDTO dto) {
        StringBuilder recommendation = new StringBuilder();
        boolean bookingStronger = false;

        if ("Booking.com".equals(dto.getRatingWinner())) {
            recommendation.append("Booking.com has the stronger guest rating.");
            bookingStronger = true;
        } else if ("Agoda.com".equals(dto.getRatingWinner())) {
            recommendation.append("Agoda.com has the stronger guest rating.");
        } else {
            recommendation.append("Ratings are comparable.");
        }

        recommendation.append(" ");

        if ("Booking.com".equals(dto.getAmenitiesWinner())) {
            recommendation.append("Booking.com also offers more unique amenities.");
            bookingStronger = true;
        } else if ("Agoda.com".equals(dto.getAmenitiesWinner())) {
            recommendation.append("Agoda.com offers more unique amenities.");
        } else {
            recommendation.append("Amenities counts are similar.");
        }

        if (!dto.getBookingOnlyAmenities().isEmpty()) {
            recommendation.append(" Suggested improvements: add ");
            recommendation.append(String.join(", ", dto.getBookingOnlyAmenities()));
            recommendation.append(".");
        }

        if (bookingStronger) {
            recommendation.append(" Overall, Booking.com listing appears stronger based on rating and amenities.");
        } else {
            recommendation.append(" Overall, Agoda.com listing appears stronger based on available comparison metrics.");
        }

        if (dto.getAgodaRating() != null && dto.getBookingRating() != null && dto.getBookingRating() - dto.getAgodaRating() >= 0.5) {
            recommendation.append(" Guest satisfaction gap detected. Focus on cleanliness, service quality and room comfort.");
        }

        return recommendation.toString().trim();
    }

    private List<String> cleanStringList(List<String> values) {
        if (values == null) {
            return null;
        }

        List<String> cleaned = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        for (String item : values) {
            String normalized = cleanValue(item);
            if (normalized == null) {
                continue;
            }

            if (normalized.length() > 120) {
                normalized = normalized.substring(0, 120).trim();
            }

            if (normalized.matches("(?i).*\\b(show more|see more|reviews|property policies|top things to do|select your room|view more|book now|breadcrumb|back to|previous page|next page|top nav|search results|about us|about this property|home|hotels)\\b.*")) {
                continue;
            }

            if (normalized.length() < 4) {
                continue;
            }

            if (seen.add(normalized)) {
                cleaned.add(normalized);
            }
        }

        return cleaned.isEmpty() ? null : cleaned;
    }

    /**
     * Converts RoomOption list to RoomOptionDTO list
     */
    public List<RoomOptionDTO> convertRoomsToDTO(List<RoomOption> rooms) {
        List<RoomOptionDTO> dtos = new ArrayList<>();
        
        if (rooms != null) {
            for (RoomOption room : rooms) {
                RoomOptionDTO dto = new RoomOptionDTO();
                dto.setRoomType(room.getRoomType());
                dto.setGuests(room.getGuests());
                dto.setPrice(room.getPrice());
                dto.setTaxesAndFees(room.getTaxesAndFees());
                dto.setAvailability(room.getAvailability());
                dto.setRoomSize(room.getRoomSize());
                dto.setBedInfo(room.getBedInfo());
                dto.setHighlights(room.getHighlights());
                dto.setAmenities(room.getAmenities());
                dto.setPolicies(room.getPolicies());
                dtos.add(dto);
            }
        }
        
        return dtos;
    }

    /**
     * Converts CompetitorAnalysis to CompetitorAnalysisDTO
     */
    /**
     * Converts competitor data to DTO — competitors are ranked by PRICE SIMILARITY
     * to the selected hotel, not by geographic proximity.
     */
    /**
     * MAX price deviation ratio allowed for a hotel to appear as a competitor.
     * e.g. 0.50 means competitors must be within ±50% of the selected hotel's price.
     * Hotels outside this band (like Radisson at ₹8,735 vs ₹2,400) are excluded.
     */
    private static final double MAX_PRICE_DEVIATION_RATIO = 0.50;

    private CompetitorAnalysisDTO convertAnalysisToDTO(Hotel hotel) {
        CompetitorAnalysisDTO dto = new CompetitorAnalysisDTO();

        List<Hotel> allCandidates = hotel.getCompetitors() == null ? List.of() : hotel.getCompetitors();
        allCandidates = filterOutSelectedHotel(allCandidates, hotel.getName());
        LOGGER.log(Level.INFO, "Competitor analysis input for hotel '{0}': selectedPrice={1}, rawCandidates={2}",
                new Object[]{safeHotelName(hotel.getName(), "Selected Hotel"), safeValue(hotel.getPrice()), allCandidates.size()});

        // ── Parse selected hotel price ──────────────────────────────────────
        double selectedPrice = parsePrice(hotel.getPrice());
        Double selectedPriceBoxed = Double.isNaN(selectedPrice) ? null : selectedPrice;

        // ── STEP 1: Hard filter — exclude hotels whose price is more than
        //            MAX_PRICE_DEVIATION_RATIO away from the selected hotel.
        //            e.g. TAG Hotels ₹2,400 → exclude anything above ₹3,600 or below ₹1,200
        List<Hotel> priceFiltered = filterByPriceBand(allCandidates, selectedPriceBoxed);

        LOGGER.log(Level.INFO,
            "Price filter: {0} total candidates → {1} within ±{2}% of ₹{3}",
            new Object[]{allCandidates.size(), priceFiltered.size(),
                (int)(MAX_PRICE_DEVIATION_RATIO * 100), selectedPriceBoxed});
        LOGGER.log(Level.INFO, "Filtered competitor names: {0}",
                new Object[]{priceFiltered.stream().map(h -> safeHotelName(h.getName(), "Unnamed")).toList()});

        // ── STEP 2: Sort remaining competitors by price proximity (closest first) ──
        List<Hotel> priceSorted = CompetitorAnalyzer.rankByPriceSimilarity(priceFiltered, selectedPriceBoxed);

        // ── Build price/rating maps: selected hotel first, then competitors ──
        Map<String, String> priceComparison  = new LinkedHashMap<>();
        Map<String, String> ratingComparison = new LinkedHashMap<>();

        String selectedName = safeHotelName(hotel.getName(), "Selected Hotel");
        priceComparison.put(selectedName, safeValue(hotel.getPrice()));
        ratingComparison.put(selectedName, safeValue(hotel.getRating()));

        List<Double> competitorPrices  = new ArrayList<>();
        List<Double> competitorRatings = new ArrayList<>();
        Set<String>  competitorAmenities = new LinkedHashSet<>();

        for (int i = 0; i < priceSorted.size(); i++) {
            Hotel c = priceSorted.get(i);
            Double cp = CompetitorAnalyzer.parsePriceAmount(c.getPrice());

            // Label showing price distance from selected hotel
            String priceDiff = "";
            if (cp != null && selectedPriceBoxed != null) {
                double diffPct = ((cp - selectedPriceBoxed) / selectedPriceBoxed) * 100.0;
                priceDiff = diffPct >= 0
                    ? String.format(" (+%.0f%%)", diffPct)
                    : String.format(" (%.0f%%)", diffPct);
            }
            String displayName = safeHotelName(c.getName(), "Competitor " + (i + 1)) + priceDiff;

            priceComparison.put(displayName, safeValue(c.getPrice()));
            ratingComparison.put(displayName, safeValue(c.getRating()));

            if (cp != null) competitorPrices.add(cp);
            double cr = parseRating(c.getRating());
            if (!Double.isNaN(cr)) competitorRatings.add(cr);
            competitorAmenities.addAll(normalizeAmenities(c.getAmenities()));
        }

        double averageCompetitorPrice  = competitorPrices.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
        double averageCompetitorRating = competitorRatings.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);

        dto.setPriceComparison(priceComparison);
        dto.setRatingComparison(ratingComparison);
        dto.setSelectedHotelPrice(selectedPriceBoxed);
        dto.setAverageCompetitorPrice(Double.isNaN(averageCompetitorPrice) ? null : averageCompetitorPrice);
        double selRating = parseRating(hotel.getRating());
        dto.setSelectedHotelRating(Double.isNaN(selRating) ? null : selRating);
        dto.setAverageCompetitorRating(Double.isNaN(averageCompetitorRating) ? null : averageCompetitorRating);

        // ── Missing amenities (only from price-similar competitors) ──────────
        Set<String> selectedAmenities = normalizeAmenities(hotel.getAmenities());
        List<String> missingAmenities = new ArrayList<>();
        for (String a : competitorAmenities) {
            if (!selectedAmenities.contains(a)) missingAmenities.add(a);
        }
        dto.setMissingAmenities(missingAmenities);

        // ── Insights ─────────────────────────────────────────────────────────
        dto.setPricingInsight(buildPricingInsight(selectedPrice, averageCompetitorPrice));
        dto.setRatingInsight(buildRatingInsight(selRating, averageCompetitorRating));

        // ── Strengths / Weaknesses / Recommendations ─────────────────────────
        // Pass only price-filtered competitors so analysis is relevant
        model.CompetitorAnalysis ca = CompetitorAnalyzer.analyze(hotel, priceFiltered);
        dto.setStrengths(ca.getStrengths());
        dto.setWeaknesses(ca.getWeaknesses());
        dto.setRecommendations(ca.getRecommendations());

        return dto;
    }

    /**
     * Returns only hotels whose price is within MAX_PRICE_DEVIATION_RATIO of
     * the selected hotel's price.
     *
     * Example: selectedPrice=₹2,400, ratio=0.50
     *   → keeps hotels between ₹1,200 and ₹3,600
     *   → excludes Radisson Blu at ₹8,735 (264% above — way outside band)
     *
     * If selectedPrice is unknown, returns all candidates unfiltered.
     */
    private List<Hotel> filterByPriceBand(List<Hotel> candidates, Double selectedPrice) {
        if (selectedPrice == null || selectedPrice <= 0 || candidates == null) {
            return candidates == null ? List.of() : candidates;
        }

        double lowerBound = selectedPrice * (1.0 - MAX_PRICE_DEVIATION_RATIO);
        double upperBound = selectedPrice * (1.0 + MAX_PRICE_DEVIATION_RATIO);

        List<Hotel> inBand    = new ArrayList<>();
        List<Hotel> outOfBand = new ArrayList<>();

        for (Hotel h : candidates) {
            Double price = CompetitorAnalyzer.parsePriceAmount(h.getPrice());
            if (price == null) {
                // No price data — include anyway (can't judge)
                inBand.add(h);
            } else if (price >= lowerBound && price <= upperBound) {
                inBand.add(h);
            } else {
                outOfBand.add(h);
                LOGGER.log(Level.INFO,
                    "Excluded competitor (price out of band): {0} at ₹{1} (band: ₹{2}–₹{3})",
                    new Object[]{h.getName(), price.intValue(),
                                 (int)lowerBound, (int)upperBound});
            }
        }

        // If ALL competitors are filtered out (very unusual market), fall back
        // to the 2 closest by price so the table is never empty
        if (inBand.isEmpty() && !outOfBand.isEmpty()) {
            LOGGER.log(Level.WARNING,
                "All competitors outside price band — falling back to 2 closest by price");
            return CompetitorAnalyzer.rankByPriceSimilarity(outOfBand, selectedPrice)
                    .stream().limit(2).collect(java.util.stream.Collectors.toList());
        }

        return inBand;
    }

    private List<Hotel> filterOutSelectedHotel(List<Hotel> competitors, String selectedHotelName) {
        if (competitors == null || competitors.isEmpty() || selectedHotelName == null || selectedHotelName.isBlank()) {
            return competitors == null ? List.of() : competitors;
        }

        String normalizedSelected = normalizeComparableName(selectedHotelName);
        List<Hotel> filtered = new ArrayList<>();

        for (Hotel competitor : competitors) {
            if (competitor == null) {
                continue;
            }
            String candidateName = competitor.getName();
            if (candidateName == null || candidateName.isBlank()) {
                filtered.add(competitor);
                continue;
            }
            String normalizedCandidate = normalizeComparableName(candidateName);
            if (normalizedCandidate.isEmpty()) {
                filtered.add(competitor);
                continue;
            }
            if (normalizedCandidate.equals(normalizedSelected)
                    || normalizedCandidate.contains(normalizedSelected)
                    || normalizedSelected.contains(normalizedCandidate)) {
                LOGGER.log(Level.INFO, "Excluding selected hotel from competitor pool: {0}", candidateName);
                continue;
            }
            filtered.add(competitor);
        }

        return filtered;
    }

    private String normalizeComparableName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String safeHotelName(String name, String fallback) {
        if (name == null || name.isBlank() || "N/A".equalsIgnoreCase(name.trim())) {
            return fallback;
        }
        return name.trim();
    }

    private String safeValue(String value) {
        if (value == null || value.isBlank()) {
            return "N/A";
        }
        return value.trim();
    }

    private Set<String> normalizeAmenities(List<String> amenities) {
        Set<String> normalized = new LinkedHashSet<>();
        if (amenities == null) {
            return normalized;
        }
        for (String amenity : amenities) {
            if (amenity != null && !amenity.isBlank()) {
                normalized.add(amenity.trim().toLowerCase(Locale.ROOT));
            }
        }
        return normalized;
    }

    private double parsePrice(String value) {
        if (value == null || value.isBlank()) {
            return Double.NaN;
        }
        String digits = value.replaceAll("[^0-9.]", "");
        if (digits.isBlank()) {
            return Double.NaN;
        }
        try {
            return Double.parseDouble(digits);
        } catch (NumberFormatException ignored) {
            return Double.NaN;
        }
    }

    private double parseRating(String value) {
        if (value == null || value.isBlank()) {
            return Double.NaN;
        }
        Matcher matcher = NUMBER_PATTERN.matcher(value);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return Double.NaN;
            }
        }
        return Double.NaN;
    }

    private String buildPricingInsight(double selectedPrice, double avgPrice) {
        if (Double.isNaN(selectedPrice) || Double.isNaN(avgPrice)) {
            return "Insufficient price data to determine if the hotel is overpriced or underpriced.";
        }
        double diffPct = ((selectedPrice - avgPrice) / avgPrice) * 100.0;
        if (diffPct > 8.0) {
            return String.format("Potentially overpriced: selected hotel is %.1f%% above nearby competitor average.", diffPct);
        }
        if (diffPct < -8.0) {
            return String.format("Potentially underpriced: selected hotel is %.1f%% below nearby competitor average.", Math.abs(diffPct));
        }
        return "Competitively priced: selected hotel is near nearby competitor average.";
    }

    private String buildRatingInsight(double selectedRating, double avgRating) {
        if (Double.isNaN(selectedRating) || Double.isNaN(avgRating)) {
            return "Insufficient rating data to benchmark against nearby competitors.";
        }
        double diff = selectedRating - avgRating;
        if (diff < -0.2) {
            return String.format("Below average rating: selected hotel rating is %.2f below nearby competitor average.", Math.abs(diff));
        }
        if (diff > 0.2) {
            return String.format("Strong rating: selected hotel rating is %.2f above nearby competitor average.", diff);
        }
        return "Rating is close to nearby competitor average.";
    }
}