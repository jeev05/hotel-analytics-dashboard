package scraper;

import model.CompetitorAnalysis;
import model.Hotel;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * CompetitorAnalyzer — Price-Based Competitor Analysis
 *
 * Competitors are ranked and filtered by PRICE PROXIMITY to the selected hotel,
 * NOT by geographic location. Hotels with prices closest to the selected hotel's
 * price are treated as the most relevant competitors.
 *
 * Price Similarity Tiers:
 *   TIER 1 (Primary)   — within 10% of selected hotel price
 *   TIER 2 (Secondary) — within 25% of selected hotel price
 *   TIER 3 (Fallback)  — all remaining, sorted by absolute price distance
 */
public class CompetitorAnalyzer {

    /** Max price deviation percentage to be considered a "similar price" competitor */
    private static final double PRIMARY_PRICE_BAND_PCT   = 0.10; // ±10%
    private static final double SECONDARY_PRICE_BAND_PCT = 0.25; // ±25%

    /** Maximum number of price-similar competitors to include in analysis */
    private static final int MAX_COMPETITORS = 8;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    /**
     * Analyse the selected hotel against its price-similar competitors.
     *
     * @param selected    the hotel being analysed
     * @param allCandidates raw competitor list scraped (any order, any price)
     * @return CompetitorAnalysis with price-ranked competitor breakdown
     */
    public static CompetitorAnalysis analyze(Hotel selected, List<Hotel> allCandidates) {
        CompetitorAnalysis analysis = new CompetitorAnalysis();

        Double selectedPrice = parsePriceAmount(selected.getPrice());

        // ── 1. RANK CANDIDATES BY PRICE PROXIMITY ──────────────────────────
        List<Hotel> priceSimilarCompetitors = rankByPriceSimilarity(allCandidates, selectedPrice);

        // ── 2. AGGREGATE METRICS ────────────────────────────────────────────
        Set<String>  selectedAmenities   = normalizeList(selected.getAmenities());
        Set<String>  competitorAmenities = new HashSet<>();
        List<Double> competitorPrices    = new ArrayList<>();
        List<Double> competitorRatings   = new ArrayList<>();
        List<Integer>competitorReviews  = new ArrayList<>();

        for (Hotel c : priceSimilarCompetitors) {
            for (String a : c.getAmenities()) {
                if (a != null && !a.isBlank()) competitorAmenities.add(a.trim().toLowerCase());
            }
            Double cp = parsePriceAmount(c.getPrice());
            if (cp != null) competitorPrices.add(cp);
            Double cr = parseRating(c.getRating());
            if (cr != null) competitorRatings.add(cr);
            Integer rv = parseReviewCount(c.getReviews());
            if (rv != null) competitorReviews.add(rv);
        }

        // ── 3. MISSING AMENITIES ────────────────────────────────────────────
        Set<String> missing = new HashSet<>(competitorAmenities);
        missing.removeAll(selectedAmenities);
        analysis.setMissingAmenities(new ArrayList<>(missing));

        // ── 4. PRICE COMPARISON (price-similarity tier labels) ───────────────
        double avgCompetitorPrice = competitorPrices.stream()
                .mapToDouble(Double::doubleValue).average().orElse(Double.NaN);

        String priceComparison = buildPriceComparisonText(selectedPrice, avgCompetitorPrice,
                priceSimilarCompetitors, allCandidates.size());
        analysis.setPriceComparison(priceComparison);

        // ── 5. RATING COMPARISON ────────────────────────────────────────────
        Double selectedRating = parseRating(selected.getRating());
        double avgCompetitorRating = competitorRatings.stream()
                .mapToDouble(Double::doubleValue).average().orElse(Double.NaN);

        String ratingComparison = buildRatingComparisonText(selectedRating, avgCompetitorRating);
        analysis.setRatingComparison(ratingComparison);

        // ── 6. REVIEW COMPARISON ────────────────────────────────────────────
        Integer selectedReviewCount = parseReviewCount(selected.getReviews());
        double  avgCompetitorReviews = competitorReviews.stream()
                .mapToInt(Integer::intValue).average().orElse(Double.NaN);

        String reviewComparison = buildReviewComparisonText(selectedReviewCount, avgCompetitorReviews);
        analysis.setReviewComparison(reviewComparison);

        // ── 7. STRENGTHS / WEAKNESSES ───────────────────────────────────────
        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();

        if (selectedRating != null && !Double.isNaN(avgCompetitorRating)) {
            if (selectedRating >= avgCompetitorRating) {
                strengths.add(String.format("Rating (%.1f) is competitive vs price-similar hotels (avg %.1f)",
                        selectedRating, avgCompetitorRating));
            } else {
                weaknesses.add(String.format("Rating (%.1f) is below price-similar competitor average (%.1f)",
                        selectedRating, avgCompetitorRating));
            }
        }

        if (!missing.isEmpty()) {
            weaknesses.add(String.format("Missing %d amenities found in similarly-priced competitors", missing.size()));
        } else if (!competitorAmenities.isEmpty()) {
            strengths.add("Amenity set matches or exceeds price-similar competitors");
        }

        if (selectedPrice != null && !Double.isNaN(avgCompetitorPrice)) {
            double priceBandUsed = getPriceBandLabel(selectedPrice, avgCompetitorPrice);
            if (priceBandUsed <= PRIMARY_PRICE_BAND_PCT) {
                strengths.add(String.format("Price (%.0f) is within ±10%% of similar-tier competitor average (%.0f)",
                        selectedPrice, avgCompetitorPrice));
            } else if (selectedPrice <= avgCompetitorPrice) {
                strengths.add("Price is competitive — lower than price-similar competitor average");
            } else {
                weaknesses.add("Price is higher than the price-similar competitor average");
            }
        }

        if (selectedReviewCount != null && !Double.isNaN(avgCompetitorReviews)) {
            if (selectedReviewCount >= avgCompetitorReviews) {
                strengths.add("Review volume is strong vs price-similar competitors");
            } else {
                weaknesses.add("Review count is lower than price-similar competitor average");
            }
        }

        analysis.setStrengths(strengths);
        analysis.setWeaknesses(weaknesses);

        // ── 8. RECOMMENDATIONS ──────────────────────────────────────────────
        List<String> recommendations = buildRecommendations(
                selectedPrice, avgCompetitorPrice,
                selectedRating, avgCompetitorRating,
                selectedReviewCount, avgCompetitorReviews,
                missing, priceSimilarCompetitors, selectedAmenities);

        analysis.setRecommendations(recommendations);

        return analysis;
    }

    // -----------------------------------------------------------------------
    // Price-Similarity Ranking
    // -----------------------------------------------------------------------

    /**
     * Ranks competitors by how close their price is to the selected hotel.
     * Returns at most MAX_COMPETITORS in ascending order of price distance.
     * If no price data exists for the selected hotel, returns candidates as-is.
     */
    public static List<Hotel> rankByPriceSimilarity(List<Hotel> candidates, Double selectedPrice) {
        if (candidates == null || candidates.isEmpty()) return Collections.emptyList();
        if (selectedPrice == null || selectedPrice <= 0) {
            // No price to compare against: return all candidates up to limit
            return candidates.stream().limit(MAX_COMPETITORS).collect(Collectors.toList());
        }

        // Compute price distances and sort
        return candidates.stream()
                .filter(h -> h != null)
                .map(h -> {
                    Double p = parsePriceAmount(h.getPrice());
                    double dist = (p != null) ? Math.abs(p - selectedPrice) : Double.MAX_VALUE;
                    return new AbstractMap.SimpleEntry<>(h, dist);
                })
                .sorted(Comparator.comparingDouble(Map.Entry::getValue))
                .limit(MAX_COMPETITORS)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Returns which price tier a competitor falls into given the selected hotel price.
     */
    public static String getPriceTierLabel(double competitorPrice, double selectedPrice) {
        if (selectedPrice <= 0) return "Unknown";
        double deviation = Math.abs(competitorPrice - selectedPrice) / selectedPrice;
        if (deviation <= PRIMARY_PRICE_BAND_PCT)   return "Primary (±10%)";
        if (deviation <= SECONDARY_PRICE_BAND_PCT)  return "Secondary (±25%)";
        return "Extended";
    }

    /** Returns pct deviation between selected and average as a ratio */
    private static double getPriceBandLabel(double selected, double avg) {
        if (avg <= 0) return Double.MAX_VALUE;
        return Math.abs(selected - avg) / avg;
    }

    // -----------------------------------------------------------------------
    // Comparison Text Builders
    // -----------------------------------------------------------------------

    private static String buildPriceComparisonText(Double selectedPrice, double avgCompetitorPrice,
                                                    List<Hotel> similar, int totalCandidates) {
        if (selectedPrice == null || Double.isNaN(avgCompetitorPrice)) {
            return "Insufficient price data for comparison.";
        }
        double diffPct = ((selectedPrice - avgCompetitorPrice) / avgCompetitorPrice) * 100.0;
        String direction = diffPct > 0 ? "higher" : "lower";

        // Count how many are in primary tier
        long primaryCount = similar.stream().filter(h -> {
            Double p = parsePriceAmount(h.getPrice());
            return p != null && Math.abs(p - selectedPrice) / selectedPrice <= PRIMARY_PRICE_BAND_PCT;
        }).count();

        return String.format(
            "Selected price (%.0f) is %.1f%% %s than the average of %d price-similar competitors (avg %.0f). " +
            "%d competitor(s) within ±10%% price band (primary tier).",
            selectedPrice, Math.abs(diffPct), direction, similar.size(), avgCompetitorPrice, primaryCount);
    }

    private static String buildRatingComparisonText(Double selectedRating, double avgRating) {
        if (selectedRating == null || Double.isNaN(avgRating)) {
            return "Insufficient rating data for comparison.";
        }
        double delta = selectedRating - avgRating;
        String dir = delta > 0 ? "above" : "below";
        return String.format("Selected rating %.2f is %.2f %s price-similar competitor average %.2f.",
                selectedRating, Math.abs(delta), dir, avgRating);
    }

    private static String buildReviewComparisonText(Integer selectedReviews, double avgReviews) {
        if (selectedReviews == null || Double.isNaN(avgReviews)) {
            return "Insufficient review data for comparison.";
        }
        double pct = ((selectedReviews - avgReviews) / Math.max(1.0, avgReviews)) * 100.0;
        String dir = pct > 0 ? "above" : "below";
        return String.format("Review count (%d) is %.0f%% %s price-similar competitor average (%.0f).",
                selectedReviews, Math.abs(pct), dir, avgReviews);
    }

    // -----------------------------------------------------------------------
    // Recommendation Engine
    // -----------------------------------------------------------------------

    private static List<String> buildRecommendations(
            Double selectedPrice, double avgPrice,
            Double selectedRating, double avgRating,
            Integer selectedReviews, double avgReviews,
            Set<String> missingAmenities,
            List<Hotel> similarCompetitors,
            Set<String> selectedAmenities) {

        List<String> recs = new ArrayList<>();

        // Price recommendation
        if (selectedPrice != null && !Double.isNaN(avgPrice)) {
            double diffPct = ((selectedPrice - avgPrice) / avgPrice) * 100.0;
            if (diffPct > 10.0) {
                recs.add(String.format(
                    "PRICE ACTION: Your rate (₹%.0f) is %.1f%% above price-similar competitor average (₹%.0f). " +
                    "Consider adding value (inclusions, early check-in) or adjusting pricing to close the gap.",
                    selectedPrice, diffPct, avgPrice));
            } else if (diffPct < -10.0) {
                recs.add(String.format(
                    "YIELD OPPORTUNITY: Your rate (₹%.0f) is %.1f%% below similar-tier competitor average (₹%.0f). " +
                    "You may be able to increase rates, especially on peak demand dates.",
                    selectedPrice, Math.abs(diffPct), avgPrice));
            } else {
                recs.add(String.format(
                    "PRICE PARITY: Your rate (₹%.0f) is within ±10%% of similar-tier competitor average (₹%.0f). " +
                    "Monitor closely to maintain parity, especially on weekends and events.",
                    selectedPrice, avgPrice));
            }
        }

        // Rating recommendation
        if (selectedRating != null && !Double.isNaN(avgRating)) {
            double delta = selectedRating - avgRating;
            if (delta < -0.2) {
                recs.add(String.format(
                    "RATING GAP: Your rating (%.2f) is %.2f below similarly-priced competitors (avg %.2f). " +
                    "Focus on guest experience improvements — housekeeping, F&B, and staff responsiveness " +
                    "are typically the highest-impact levers.",
                    selectedRating, Math.abs(delta), avgRating));
            } else if (delta > 0.2) {
                recs.add(String.format(
                    "RATING ADVANTAGE: Your rating (%.2f) is %.2f above price-similar competitors (avg %.2f). " +
                    "Highlight this in OTA listings and marketing materials to justify any price premium.",
                    selectedRating, delta, avgRating));
            }
        }

        // Review volume recommendation
        if (selectedReviews != null && !Double.isNaN(avgReviews)) {
            double pct = ((selectedReviews - avgReviews) / Math.max(1.0, avgReviews)) * 100.0;
            if (pct < -20.0) {
                recs.add(String.format(
                    "REVIEW VOLUME: Your review count (%d) is %.0f%% below price-similar competitor average (%.0f). " +
                    "Launch a post-stay email sequence to encourage reviews. Even 10–15 extra reviews/month " +
                    "can meaningfully improve OTA visibility.",
                    selectedReviews, Math.abs(pct), avgReviews));
            } else if (pct > 20.0) {
                recs.add(String.format(
                    "REVIEW STRENGTH: Your review count (%d) is %.0f%% above price-similar competitors (%.0f). " +
                    "Showcase this social proof on your listing pages.",
                    selectedReviews, pct, avgReviews));
            }
        }

        // Amenity recommendation
        if (!missingAmenities.isEmpty()) {
            List<String> topMissing = new ArrayList<>(missingAmenities);
            if (topMissing.size() > 5) topMissing = topMissing.subList(0, 5);
            recs.add(String.format(
                "AMENITY GAP: %d amenity(ies) offered by similarly-priced competitors are absent from your listing: %s. " +
                "Adding or listing these could improve conversion.",
                missingAmenities.size(), String.join(", ", topMissing)));
        }

        // Content recommendation if many competitors are ranked
        if (similarCompetitors.size() >= 5) {
            recs.add("CONTENT AUDIT: With " + similarCompetitors.size() + " price-similar competitors identified, " +
                    "ensure your OTA descriptions, photo count, and room-type detail pages are fully updated. " +
                    "Incomplete listings lose to better-photographed, fully-described alternatives at the same price point.");
        }

        return recs;
    }

    // -----------------------------------------------------------------------
    // Parsing Utilities
    // -----------------------------------------------------------------------

    public static Double parsePriceAmount(String priceText) {
        if (priceText == null || priceText.isBlank()) return null;
        String cleaned = priceText.replaceAll("₹|\\$|€|£|Rs\\.?|,", "").trim();
        Matcher m = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)").matcher(cleaned);
        if (m.find()) {
            try { return Double.parseDouble(m.group(1)); }
            catch (NumberFormatException ignored) {}
        }
        return null;
    }

    static Double parseRating(String ratingText) {
        if (ratingText == null || ratingText.isBlank()) return null;
        Matcher m = Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(ratingText);
        if (m.find()) {
            try { return Double.parseDouble(m.group(1)); }
            catch (NumberFormatException ignored) {}
        }
        return null;
    }

    static Integer parseReviewCount(String reviewText) {
        if (reviewText == null || reviewText.isBlank()) return null;
        String cleaned = reviewText.replaceAll("[^0-9,]", "");
        if (cleaned.isBlank()) return null;
        try { return Integer.parseInt(cleaned.replaceAll(",", "")); }
        catch (NumberFormatException ignored) {}
        return null;
    }

    private static Set<String> normalizeList(List<String> list) {
        Set<String> out = new HashSet<>();
        if (list == null) return out;
        for (String v : list) {
            if (v != null && !v.isBlank()) out.add(v.trim().toLowerCase());
        }
        return out;
    }
}