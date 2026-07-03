package scraper;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import model.CompetitorAnalysis;
import model.Hotel;
import model.RoomOption;
import com.hotelapi.service.ImageAnalyzer;

public class HotelScraper {
    private static final Logger LOGGER = Logger.getLogger(HotelScraper.class.getName());
    private static final String BOOKING_BASE_URL = "https://www.booking.com";
    private static final String PROPERTY_CARD_SELECTOR = "[data-testid='property-card']";
        private static final String[] ROOM_TABLE_SELECTORS = {
            "#hprt-table",
            "table.hprt-table",
            "[data-testid='rooms-table']",
            "[id*='room_grid']",
            "table:has(select)"
        };
    private static final int SEARCH_TIMEOUT_MS = 15000;
    private static final int MAX_RESULTS_TO_SCORE = 5;
    private static final int MAX_AMENITIES = 12;
    private static final int DEFAULT_STAY_NIGHTS = 1;
    private static final int[] DATE_RETRY_OFFSETS = {7, 14, 30, 60, 90};
        private static final Pattern PRICE_PATTERN = Pattern.compile(".*[₹$€£].*\\d.*");
        private static final Pattern ROOM_SIZE_PATTERN = Pattern.compile(".*(m²|sq\\.? ?ft|square).*");
        private static final Pattern BED_PATTERN = Pattern.compile(".*\\bbed\\b.*");
        private static final Pattern TAX_PATTERN = Pattern.compile(".*(tax|fee).*", Pattern.CASE_INSENSITIVE);

    public static void main(String[] args) {
        configureLogging();

        String hotelName = promptForHotelName();
        if (hotelName == null) {
            return;
        }

        Hotel hotel = scrapeHotelByName(hotelName);
        if (hotel == null) {
            LOGGER.severe("Could not extract hotel details from Booking.com.");
            return;
        }

        LOGGER.log(Level.INFO, () -> System.lineSeparator() + hotel.toDisplayString());
    }

    private static void configureLogging() {
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.INFO);
        for (Handler handler : rootLogger.getHandlers()) {
            handler.setLevel(Level.INFO);
        }
    }

    private static String promptForHotelName() {
        try (Scanner scanner = new Scanner(System.in)) {
            LOGGER.info("Enter hotel name (e.g., Le Meridian): ");
            String hotelName = scanner.nextLine().trim();
            if (hotelName.isEmpty()) {
                LOGGER.warning("Hotel name cannot be empty.");
                return null;
            }
            return hotelName;
        }
    }

    public static Hotel scrapeHotelByName(String hotelName) {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
             BrowserContext context = browser.newContext(
                     new Browser.NewContextOptions().setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                     .setLocale("en-US")
                     .setExtraHTTPHeaders(java.util.Map.ofEntries(
                         java.util.Map.entry("Accept-Language", "en-US,en;q=0.9"),
                         java.util.Map.entry("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"),
                         java.util.Map.entry("Accept-Encoding", "gzip, deflate, br"),
                         java.util.Map.entry("Cache-Control", "max-age=0"),
                         java.util.Map.entry("Sec-Ch-Ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\""),
                         java.util.Map.entry("Sec-Ch-Ua-Mobile", "?0"),
                         java.util.Map.entry("Sec-Ch-Ua-Platform", "\"Windows\""),
                         java.util.Map.entry("Sec-Fetch-Dest", "document"),
                         java.util.Map.entry("Sec-Fetch-Mode", "navigate"),
                         java.util.Map.entry("Sec-Fetch-Site", "none"),
                         java.util.Map.entry("Sec-Fetch-User", "?1"),
                         java.util.Map.entry("Upgrade-Insecure-Requests", "1")
                     )));
             Page page = context.newPage()) {

            LOGGER.log(Level.INFO, "Searching for hotel: {0} on Booking.com...", hotelName);
            String searchUrl = buildSearchUrl(hotelName);
            LOGGER.log(Level.INFO, "Search URL: {0}", searchUrl);

            // Navigate to English version first to set language preference
            page.navigate("https://www.booking.com/en-us/index.html");
            page.waitForLoadState();
            page.waitForTimeout(2000);

            // Set language cookie
            page.context().addCookies(java.util.Arrays.asList(
                new com.microsoft.playwright.options.Cookie("language", "en-us")
                    .setDomain("booking.com")
                    .setPath("/")
            ));

            page.navigate(searchUrl);
            dismissPopups(page);

            // Wait for page to load and check if we're on the right page
            page.waitForLoadState();
            String currentUrl = page.url();
            LOGGER.log(Level.INFO, "Current URL after navigation: {0}", currentUrl);

            // Check if we got redirected to a hotel page directly
            if (currentUrl.contains("/hotel/") || currentUrl.contains("/hotels/")) {
                LOGGER.log(Level.INFO, "Redirected directly to hotel page, extracting data...");
                return extractHotelDetails(page, hotelName, null);
            }

            // Wait for search results
            try {
                page.waitForSelector(PROPERTY_CARD_SELECTOR, new Page.WaitForSelectorOptions().setTimeout(SEARCH_TIMEOUT_MS));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Property card selector not found within timeout. Page content preview:");
                String pageTitle = page.title();
                LOGGER.log(Level.WARNING, "Page title: {0}", pageTitle);

                // Try to get some page content for debugging
                try {
                    String bodyText = page.locator("body").textContent();
                    String preview = bodyText.length() > 200 ? bodyText.substring(0, 200) + "..." : bodyText;
                    LOGGER.log(Level.WARNING, "Page content preview: {0}", preview);
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "Could not get page content: {0}", ex.getMessage());
                }

                return null;
            }

            page.waitForTimeout(2000);

            SearchMatchResult searchResult = navigateToBestMatch(page, hotelName);
            if (searchResult == null || searchResult.bestUrl == null) {
                return null;
            }

            dismissPopups(page);
            page.waitForTimeout(4000);
            Hotel hotel = extractHotelDetails(page, hotelName, searchResult.bestUrl);
            hotel.setCompetitors(searchResult.competitors);

            // Competitor analysis module
            if (searchResult.competitors != null && !searchResult.competitors.isEmpty()) {
                CompetitorAnalysis analysis = CompetitorAnalyzer.analyze(hotel, searchResult.competitors);
                String analysisText = analysis.toDisplayString();
                hotel.setCompetitorInsights(analysisText);
                LOGGER.info(analysisText);
            } else {
                hotel.setCompetitorInsights(null);
                LOGGER.info("No competitor data available for analysis.");
            }

            return hotel;
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Error during scraping", exception);
            return null;
        }
    }

    private static class SearchMatchResult {
        String bestUrl;
        List<Hotel> competitors;

        SearchMatchResult(String bestUrl, List<Hotel> competitors) {
            this.bestUrl = bestUrl;
            this.competitors = competitors;
        }
    }

    private static SearchMatchResult navigateToBestMatch(Page page, String hotelName) {
        page.navigate(buildSearchUrl(hotelName));
        dismissPopups(page);
        page.waitForSelector(PROPERTY_CARD_SELECTOR, new Page.WaitForSelectorOptions().setTimeout(SEARCH_TIMEOUT_MS));
        page.waitForTimeout(2000);

        Locator cards = page.locator(PROPERTY_CARD_SELECTOR);
        int hotelCount = cards.count();
        if (hotelCount == 0) {
            LOGGER.log(Level.WARNING, "No hotels found matching: {0}. This could mean the hotel doesn't exist on Booking.com or the search query needs refinement.", hotelName);
            return null;
        }

        int bestIndex = -1;
        String bestUrl = null;
        double bestScore = -1;

        for (int index = 0; index < Math.min(hotelCount, MAX_RESULTS_TO_SCORE); index++) {
            Locator card = cards.nth(index);
            String title = firstNonBlank(
                    extractText(card, "[data-testid='title']"),
                    extractText(card, "div[role='heading']"),
                    extractText(card, "a[data-testid='title-link']")
            );

            String href = firstNonBlank(
                    extractAttribute(card, "a[data-testid='title-link']", "href"),
                    extractAttribute(card, "a", "href")
            );

            if (title == null || href == null) {
                continue;
            }

            double score = similarityScore(hotelName, title);
            if (score > bestScore) {
                bestScore = score;
                bestUrl = normalizeBookingUrl(href);
                bestIndex = index;
            }
        }

        if (bestUrl == null) {
            return null;
        }

        // Pick a broader set of candidate competitors from search results so
        // price-similar matches can be chosen later by the analysis module.
        List<Hotel> competitors = extractCompetitorsFromSearch(cards, bestIndex, 12);

        long roundedScore = Math.round(bestScore);
        LOGGER.log(Level.INFO, "Opening best matching hotel... score={0}", roundedScore);
        page.navigate(bestUrl);

        return new SearchMatchResult(bestUrl, competitors);
    }

    private static List<Hotel> extractCompetitorsFromSearch(Locator cards, int selectedIndex, int maxCompetitors) {
        List<Hotel> competitors = new ArrayList<>();

        int count = cards.count();
        for (int index = 0; index < count && competitors.size() < maxCompetitors; index++) {
            if (index == selectedIndex) {
                continue;
            }

            try {
                Locator card = cards.nth(index);
                Hotel competitor = new Hotel();

                String name = firstNonBlank(
                        extractText(card, "[data-testid='title']"),
                        extractText(card, "div[role='heading']"),
                        extractText(card, "a[data-testid='title-link']"),
                        extractText(card, "a")
                );
                competitor.setName(firstNonBlank(name));

                competitor.setPrice(firstNonBlank(
                    extractText(card, "[data-testid='price-and-discounted-price']"),
                    extractText(card, "span[class*='price']"),
                    extractText(card, "[data-testid='price']")
                ));

                competitor.setRating(firstNonBlank(
                    extractText(card, "[data-testid='review-score-component']"),
                    extractText(card, "[data-testid='review-score']"),
                    extractText(card, "div[class*='review-score']")
                ));

                competitor.setReviews(firstNonBlank(
                    extractText(card, "[data-testid='review-score-right-component']"),
                    extractText(card, "[class*='review-count']")
                ));

                competitor.setAmenities(extractCardAmenities(card));
                competitor.setSourceUrl(firstNonBlank(
                        extractAttribute(card, "a[data-testid='title-link']", "href"),
                        extractAttribute(card, "a", "href"),
                        "N/A"
                ));

                competitors.add(competitor);
            } catch (Exception ignored) {
                // continue to the next card if scraping one fails.
            }
        }

        return competitors;
    }
      private static List<String> extractGalleryImages(Page page) {
    List<String> imageUrls = new ArrayList<>();

    try {
        page.waitForTimeout(4000);

        // target all hotel images
        Locator images = page.locator("img");

        for (int i = 0; i < images.count(); i++) {

            String src = images.nth(i).getAttribute("src");

            if (src != null &&
                src.contains("bstatic.com") &&
                src.contains("/hotel/") &&   // 🔥 IMPORTANT
                !src.contains("square60") &&
                !src.contains("flags") &&
                !src.contains("icons")) {

                imageUrls.add(src);
            }

            // fallback srcset
            String srcset = images.nth(i).getAttribute("srcset");
            if (srcset != null && srcset.contains("/hotel/")) {
                String url = srcset.split(" ")[0];
                imageUrls.add(url);
            }
        }

    } catch (Exception e) {
        System.out.println("Gallery extraction failed");
    }

    return imageUrls;
}
    private static List<String> extractCardAmenities(Locator card) {
        Set<String> amenities = new LinkedHashSet<>();
        String[] selectors = {
                "[data-testid='important_facility'] li",
                "[data-testid='hotel_features'] li",
                "li[class*='facility']",
                "div[class*='facility']",
                "span[class*='amenity']"
        };

        for (String selector : selectors) {
            try {
                for (String text : card.locator(selector).allTextContents()) {
                    String normalized = normalizeText(text);
                    if (normalized != null && !normalized.isBlank()) {
                        amenities.add(normalized);
                    }
                }
            } catch (Exception ignored) {
                // ignore and continue with other selectors.
            }
        }

        return new ArrayList<>(amenities);
    }

    private static Hotel extractHotelDetails(Page page, String hotelName, String propertyUrl) {
        Hotel hotel = new Hotel();
        hotel.setSearchQuery(hotelName);
        hotel.setSourceUrl(firstNonBlank(page.url(), propertyUrl));

        List<String> jsonLdBlocks = page.locator("script[type='application/ld+json']").allTextContents();

        hotel.setName(firstNonBlank(
                extractJsonLdField(jsonLdBlocks, "name"),
                extractMetaContent(page, "meta[property='og:title']"),
                extractText(page, "h1"),
                "N/A"
        ));

        hotel.setAddress(cleanHindiText(firstNonBlank(
            extractAddressFromJsonLd(jsonLdBlocks),
            extractText(page, "[data-testid='address']"),
            extractText(page, "span[data-testid='address']"),
            extractText(page, "div[class*='address']"),
            extractText(page, "a[data-atlas-latlng]"),
            extractText(page, "span[class*='hp_address_subtitle']"),
            extractText(page, "span.hp_address_subtitle"),
            extractText(page, "div.hp_address_subtitle"),
            extractText(page, "div[data-node_tt_id='location_score_tooltip']"),
            extractText(page, "#showMap2 span"),
            extractText(page, "#showMap2"),
            extractText(page, "[class*='address'] span"),
            extractText(page, "[class*='address']"),
            extractText(page, "body"), // fallback: try to parse from visible text
            extractAddressFromVisibleText(page),
            "N/A"
        )));

        hotel.setPrice(firstNonBlank(
            extractJsonLdField(jsonLdBlocks, "priceRange"),
            extractFirstPrice(page)
        ));

        hotel.setRating(firstNonBlank(
            extractJsonLdField(jsonLdBlocks, "ratingValue"),
            extractRatingText(page)
        ));

        hotel.setReviews(cleanHindiText(firstNonBlank(
            extractJsonLdField(jsonLdBlocks, "reviewCount"),
            extractReviewText(page)
        )));

        hotel.setDescription(cleanHindiText(firstNonBlank(
            extractMetaContent(page, "meta[name='description']"),
            extractText(page, "[data-testid='property-description']"),
            extractText(page, "div[class*='property_description']"),
            extractText(page, "p[class*='description']")
        )));
        List<String> images = extractGalleryImages(page);

System.out.println("TOTAL IMAGES: " + images.size());

// set first as hero
if (!images.isEmpty()) {
    hotel.setImageUrl(images.get(0));
}
        hotel.setAmenities(extractAmenities(page));
        hotel.setHouseRules(extractHouseRules(page));
        hotel.setRoomOptions(extractRoomOptionsWithRetries(page, propertyUrl));
        hotel.setSourceUrl(firstNonBlank(page.url(), propertyUrl));
        hotel.setPrice(resolveDisplayPrice(hotel.getPrice(), hotel.getRoomOptions(), page));
        ImageAnalyzer.analyzeMultiple(images, hotel);
        return hotel;

    }

    private static String resolveDisplayPrice(String currentPrice, List<RoomOption> roomOptions, Page page) {
        String fromRooms = pickRepresentativeRoomPrice(roomOptions);
        if (fromRooms != null) {
            return fromRooms;
        }

        String fromPage = extractFirstPrice(page);
        if (fromPage != null) {
            return fromPage;
        }

        return firstNonBlank(currentPrice, null);
    }

    private static String pickRepresentativeRoomPrice(List<RoomOption> roomOptions) {
        String bestPrice = null;
        int bestValue = Integer.MAX_VALUE;

        for (RoomOption option : roomOptions) {
            String optionPrice = normalizeText(option.getPrice());
            if (!isValidDisplayPrice(optionPrice)) {
                continue;
            }

            Integer numeric = parsePriceAmount(optionPrice);
            if (numeric != null && numeric < bestValue) {
                bestValue = numeric;
                bestPrice = optionPrice;
            }
        }

        return bestPrice;
    }

    private static List<RoomOption> extractRoomOptionsWithRetries(Page page, String propertyUrl) {
        List<RoomOption> options = extractRoomOptions(page);
        if (!options.isEmpty()) {
            return options;
        }

        if (!isNoAvailabilityState(page)) {
            return options;
        }

        LOGGER.info("Room inventory unavailable for current dates, trying alternate date windows...");
        for (int offset : DATE_RETRY_OFFSETS) {
            try {
                String retriedUrl = withDateRange(propertyUrl, LocalDate.now().plusDays(offset), DEFAULT_STAY_NIGHTS);
                page.navigate(retriedUrl);
                dismissPopups(page);
                page.waitForTimeout(2500);

                options = extractRoomOptions(page);
                if (!options.isEmpty()) {
                    LOGGER.log(Level.INFO, "Found room options using date offset +{0} day(s).", offset);
                    return options;
                }
            } catch (Exception retryException) {
                LOGGER.log(Level.FINE, retryException,
                        () -> String.format("Date retry failed at offset %d", offset));
            }
        }

        return options;
    }

    private static boolean isNoAvailabilityState(Page page) {
        String url = firstNonBlank(page.url(), "");
        if (url == null) {
            url = "";
        }
        if (url.contains("#no_availability_msg")) {
            return true;
        }

        String pageText = firstNonBlank(extractText(page, "body"), "");
        if (pageText == null) {
            pageText = "";
        }
        pageText = pageText.toLowerCase(Locale.ROOT);
        return pageText.contains("no availability") || pageText.contains("sold out") || pageText.contains("not available for your dates");
    }

    private static String withDateRange(String url, LocalDate checkIn, int nights) {
        LocalDate checkOut = checkIn.plusDays(nights);
        String updated = stripFragment(url);
        updated = replaceOrAppendQueryParam(updated, "checkin", checkIn.format(DateTimeFormatter.ISO_LOCAL_DATE));
        updated = replaceOrAppendQueryParam(updated, "checkout", checkOut.format(DateTimeFormatter.ISO_LOCAL_DATE));
        updated = replaceOrAppendQueryParam(updated, "group_adults", "2");
        updated = replaceOrAppendQueryParam(updated, "no_rooms", "1");
        updated = replaceOrAppendQueryParam(updated, "group_children", "0");
        return updated;
    }

    private static String stripFragment(String url) {
        int fragmentIndex = url.indexOf('#');
        return fragmentIndex >= 0 ? url.substring(0, fragmentIndex) : url;
    }

    private static String replaceOrAppendQueryParam(String url, String key, String value) {
        String pattern = "([?&])" + key + "=[^&]*";
        if (url.matches(".*" + pattern + ".*")) {
            return url.replaceAll(pattern, "$1" + key + "=" + value);
        }
        return url + (url.contains("?") ? "&" : "?") + key + "=" + value;
    }

    private static void dismissPopups(Page page) {
        clickIfVisible(page, "button[aria-label='Dismiss sign-in info.']");
        clickIfVisible(page, "button[aria-label='Dismiss sign-in prompt.']");
        clickIfVisible(page, "button[aria-label='Close']");
        clickIfVisible(page, "#onetrust-accept-btn-handler");
        clickIfVisible(page, "button:has-text('Accept')");
    }

    private static void clickIfVisible(Page page, String selector) {
        try {
            Locator locator = page.locator(selector);
            if (locator.count() > 0 && locator.first().isVisible()) {
                locator.first().click(new Locator.ClickOptions().setTimeout(2000));
            }
        } catch (Exception ignored) {
            // Popup presence varies per page and region.
        }
    }

    private static String extractText(Page page, String selector) {
        return extractText(page.locator(selector));
    }

    private static String extractText(Locator root, String selector) {
        return extractText(root.locator(selector));
    }

    private static String extractText(Locator locator) {
        try {
            if (locator.count() == 0) {
                return null;
            }
            return normalizeText(locator.first().innerText(new Locator.InnerTextOptions().setTimeout(2000)));
        } catch (Exception ignored) {
            // Selector may not exist in this Booking page variant.
            return null;
        }
    }

    private static String extractAttribute(Locator root, String selector, String attributeName) {
        try {
            Locator locator = root.locator(selector);
            if (locator.count() == 0) {
                return null;
            }
            return normalizeText(locator.first().getAttribute(attributeName));
        } catch (Exception ignored) {
            // Attribute lookup can fail on optional content.
            return null;
        }
    }

    private static String extractMetaContent(Page page, String selector) {
        try {
            Locator locator = page.locator(selector);
            if (locator.count() == 0) {
                return null;
            }
            return normalizeText(locator.first().getAttribute("content"));
        } catch (Exception ignored) {
            // Meta tags are not guaranteed to exist on every page.
            return null;
        }
    }

    private static String extractJsonLdField(List<String> jsonLdBlocks, String fieldName) {
        Pattern pattern = buildJsonPattern(fieldName);
        for (String block : jsonLdBlocks) {
            Matcher matcher = pattern.matcher(block);
            if (matcher.find()) {
                return normalizeText(unescapeJson(matcher.group(1)));
            }
        }
        return null;
    }

    private static String extractAddressFromJsonLd(List<String> jsonLdBlocks) {
        for (String block : jsonLdBlocks) {
            String street = extractField(block, "streetAddress");
            String locality = extractField(block, "addressLocality");
            String region = extractField(block, "addressRegion");
            String postalCode = extractField(block, "postalCode");
            String country = extractField(block, "addressCountry");
            String address = joinNonBlank(", ", street, locality, region, postalCode, country);
            if (address != null) {
                return address;
            }
        }
        return null;
    }

    private static String extractField(String block, String fieldName) {
        Matcher matcher = buildJsonPattern(fieldName).matcher(block);
        if (matcher.find()) {
            return normalizeText(unescapeJson(matcher.group(1)));
        }
        return null;
    }

    private static Pattern buildJsonPattern(String fieldName) {
        return Pattern.compile("\\\"" + Pattern.quote(fieldName) + "\\\"\\s*:\\s*\\\"(.*?)\\\"");
    }

    private static String extractAddressFromVisibleText(Page page) {
        List<String> candidates = new ArrayList<>();
        candidates.add(extractText(page, "div[class*='location']"));
        candidates.add(extractText(page, "span[class*='location']"));
        candidates.add(extractText(page, "div[class*='hp_address_subtitle']"));
        for (String candidate : candidates) {
            if (looksLikeAddress(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static String extractFirstPrice(Page page) {
        List<String> candidates = new ArrayList<>();
        candidates.add(extractText(page, "[data-testid='price-for-x-nights']"));
        candidates.add(extractText(page, "[data-testid='price-and-discounted-price']"));
        candidates.add(extractText(page, "span[class*='prco']"));
        candidates.add(extractText(page, "[class*='bui-price']"));
        candidates.add(extractText(page, "#hprt-table td"));

        for (String candidate : candidates) {
            String normalized = normalizeText(candidate);
            if (isValidDisplayPrice(normalized)) {
                return normalized;
            }
        }

        try {
            for (String text : page.locator("span").allTextContents()) {
                String normalized = normalizeText(text);
                if (isValidDisplayPrice(normalized)) {
                    return normalized;
                }
            }
        } catch (Exception ignored) {
            // Some pages render price blocks lazily.
        }
        return null;
    }

    private static String extractRatingText(Page page) {
        String ratingText = firstNonBlank(
                extractText(page, "[data-testid='review-score-component']"),
                extractText(page, "[data-testid='review-score-right-component']"),
                extractText(page, "div[class*='review-score']")
        );

        if (ratingText == null) {
            return null;
        }

        Matcher matcher = Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(ratingText);
        return matcher.find() ? matcher.group(1) : ratingText;
    }

    private static String extractReviewText(Page page) {

    try {

        Locator review = page.locator("text=/\\d+\\s+reviews?/").first();

        if (review.count() > 0) {

            Matcher m = Pattern.compile("(\\d+)\\s+reviews?")
                    .matcher(review.innerText());

            if (m.find()) {
                return m.group(1);
            }
        }

    } catch (Exception ignored) {
    }

    return null;
}

    private static List<String> extractAmenities(Page page) {
        Set<String> amenities = new LinkedHashSet<>();
        collectAmenities(page, amenities, "[data-testid='property-most-popular-facilities-wrapper'] li");
        collectAmenities(page, amenities, "[data-testid='facilities-list'] li");
        collectAmenities(page, amenities, "div[class*='facility_block']");
        collectAmenities(page, amenities, "[class*='facility']");
        System.out.println("AMENITIES EXTRACTED: " + amenities.size() + " -> " + amenities);
        return new ArrayList<>(amenities);
    }

    private static List<String> extractHouseRules(Page page) {

    List<String> houseRules = new ArrayList<>();

    try {

        // Booking.com's markup for this section varies (h2, h3, or a plain div/span acting as
        // a heading depending on the property/locale variant), so try several heading strategies
        // instead of relying on a single hardcoded "h2:has-text(...)" selector.
        String[] headingSelectors = {
                "h2:has-text('House rules')",
                "h3:has-text('House rules')",
                "body :text-is('House rules')",
                "body :text('House rules')"
        };

        Locator heading = null;
        for (String selector : headingSelectors) {
            try {
                Locator candidate = page.locator(selector);
                if (candidate.count() > 0) {
                    heading = candidate;
                    break;
                }
            } catch (Exception ignored) {
                // Try the next selector strategy.
            }
        }

        if (heading == null) {
            System.out.println("House Rules heading not found");
            return houseRules;
        }

        System.out.println("House Rules heading found, count=" + heading.count());

        try {
            heading.first().scrollIntoViewIfNeeded();
        } catch (Exception scrollEx) {
            System.out.println("House Rules scrollIntoViewIfNeeded failed: " + scrollEx.getMessage());
        }
        page.waitForTimeout(800);

        // CONFIRMED STRUCTURE (found via direct DOM inspection): the heading sits inside a
        // container, and its NEXT SIBLING is <div data-testid="property-section--content">,
        // which holds the actual rule rows. Each row has an icon, a short title (e.g.
        // "Check-in", "Pets", "Cards accepted at this hotel"), and the rest of the row is the
        // value/description. We target this container directly via Playwright locators and read
        // each row's text via innerText() scoped to that specific row element — this avoids the
        // earlier full-body slicing approach entirely, which kept missing the content for reasons
        // that are no longer relevant now that we know exactly where it lives.
        Locator sectionContent = null;
        try {
            // CONFIRMED from direct DOM inspection: the heading's own small header wrapper and
            // <div data-testid="property-section--content"> (which holds the actual rule rows)
            // are BOTH direct children of the same shared ancestor, a few levels up from the
            // heading. Walk up to that shared ancestor first, then scope the content-container
            // lookup to within it — page.locator("[data-testid='property-section--content']")
            // alone is too broad since other unrelated sections on the page reuse that same
            // generic testid.
            Locator sharedAncestor = heading.first().locator(
                    "xpath=ancestor::*[.//*[@data-testid='property-section--content']][1]");
            Locator candidate = sharedAncestor.locator("[data-testid='property-section--content']").first();
            if (candidate.count() > 0) {
                sectionContent = candidate;
            }
            System.out.println("House Rules property-section--content found: " + (sectionContent != null));
        } catch (Exception findEx) {
            System.out.println("House Rules property-section--content lookup failed: " + findEx.getMessage());
        }

        List<String> cleaned = new ArrayList<>();

        if (sectionContent != null) {
            try {
                // Each direct child of the content container is one "row" — but on this page all
                // 7 rules turned out to be nested inside a SINGLE wrapper div (row count=1), not
                // one div per rule as first assumed. That's fine: the row's innerText() already
                // contains the full, correctly-ordered text for every rule, separated by real
                // newlines (confirmed: "Check-in\nAvailable 24 hours\nCheck-out\n..."). Splitting
                // only on the FIRST newline and flattening the rest was the bug — it merged
                // everything after the first title into one giant value string. Split on EVERY
                // newline instead and let the title/value pairing loop below do the grouping,
                // exactly as it already does correctly for the rest of this method.
                Locator rows = sectionContent.locator("> div");
                int rowCount = rows.count();
                System.out.println("House Rules row count=" + rowCount);
                for (int i = 0; i < rowCount; i++) {
                    try {
                        String rowText = rows.nth(i).innerText();
                        System.out.println("House Rules row " + i + " text: " + (rowText == null ? "null" : rowText.replace("\n", " | ")));
                        if (rowText != null && !rowText.isBlank()) {
                            for (String line : rowText.split("\\r?\\n")) {
                                String trimmed = line.trim();
                                if (!trimmed.isEmpty()) {
                                    cleaned.add(trimmed);
                                }
                            }
                        }
                    } catch (Exception rowEx) {
                        System.out.println("House Rules row " + i + " read failed: " + rowEx.getMessage());
                    }
                }
            } catch (Exception rowsEx) {
                System.out.println("House Rules rows extraction failed: " + rowsEx.getMessage());
            }
        }

        if (cleaned.isEmpty()) {
            System.out.println("House Rules structured row extraction produced nothing; section unavailable");
            return houseRules;
        }

        // Convert every title + value into one line. A "title" is a short heading-like line
        // (Check-in, Pets, Cancellation/ prepayment, etc.); everything until the next title is
        // its value. Lines that are too long to be a title are folded into the *previous* title's
        // value instead of being skipped outright, so nothing gets silently dropped or misaligned.
        String currentTitle = null;
        StringBuilder currentValue = new StringBuilder();

        for (String line : cleaned) {

            boolean looksLikeTitle = line.length() <= 40
                    && line.matches("(?i)^(Check-in|Check-out|Cancellation/?\\s*prepayment|Children\\s*&\\s*Beds|Age restriction|No age restriction|Pets|Groups|Cards accepted(?: at this (?:hotel|property))?)$");

            if (looksLikeTitle) {
                if (currentTitle != null && currentValue.length() > 0) {
                    houseRules.add(currentTitle + " : " + currentValue.toString().trim());
                }
                currentTitle = line;
                currentValue = new StringBuilder();
            } else if (currentTitle != null) {
                if (currentValue.length() > 0) {
                    currentValue.append(" ");
                }
                currentValue.append(line);
            }
            // Lines before any title is found are part of the heading area and are dropped.
        }

        if (currentTitle != null && currentValue.length() > 0) {
            houseRules.add(currentTitle + " : " + currentValue.toString().trim());
        }

        // Safety net: if the title/value pairing above didn't match Booking.com's actual
        // structure for this property and produced nothing, fall back to the raw cleaned
        // lines (deduped, length-filtered) rather than showing no house rules at all.
        if (houseRules.isEmpty() && !cleaned.isEmpty()) {
            System.out.println("House rules title/value pairing produced 0 rows; falling back to raw lines");
            Set<String> fallback = new LinkedHashSet<>();
            for (String line : cleaned) {
                if (line.length() >= 3 && line.length() <= 300) {
                    fallback.add(line);
                }
            }
            houseRules.addAll(fallback);
        }

    } catch (Exception e) {
        System.out.println("House Rules extraction failed with exception: " + e);
        LOGGER.warning("House Rules extraction failed: " + e.getMessage());
    }

    return houseRules;
}


    private static void collectHouseRuleLines(Set<String> houseRules, String sectionText) {
        if (sectionText == null || sectionText.isBlank()) {
            return;
        }

        // First, split on natural markers and bullet-like separators.
        for (String rawLine : sectionText.split("\\r?\\n")) {
            if (rawLine == null || rawLine.isBlank()) {
                continue;
            }
            String line = normalizeText(rawLine);
            if (line == null) {
                continue;
            }
            String[] ruleParts = line.split("\\s*[\\u2022\\u2023\\u25E6\\u2024\\-–—•;:]\\s*");
            for (String part : ruleParts) {
                String candidate = normalizeText(part);
                if (candidate != null && candidate.length() > 2 && !isNoise(candidate)) {
                    houseRules.add(candidate);
                }
            }
        }
    }

    private static String findHouseRulesUrl(Page page) {
        String[] selectors = {
                "a:has-text('House rules')",
                "a:has-text('policies')",
                "a[href*='house_rules']",
                "a[href*='policy']",
                "a[href*='hotel_policy']"
        };

        for (String selector : selectors) {
            try {
                Locator link = page.locator(selector).first();
                if (link != null && link.count() > 0) {
                    String href = normalizeText(link.getAttribute("href"));
                    if (href != null && !href.isBlank()) {
                        return normalizeBookingUrl(href);
                    }
                }
            } catch (Exception ignored) {
                // Try next selector.
            }
        }
        return null;
    }

    private static void collectAmenities(Page page, Set<String> amenities, String selector) {
        try {
            int matchCount = page.locator(selector).count();
            System.out.println("AMENITIES selector '" + selector + "' matched " + matchCount + " element(s)");
            for (String text : page.locator(selector).allTextContents()) {
                String normalized = normalizeText(text);
                if (normalized != null && normalized.length() <= 80) {
                    amenities.add(normalized);
                }
                if (amenities.size() >= MAX_AMENITIES) {
                    return;
                }
            }
        } catch (Exception e) {
            System.out.println("AMENITIES selector '" + selector + "' failed: " + e.getMessage());
        }
    }

    private static List<RoomOption> extractRoomOptions(Page page) {
        waitForRoomSection(page);

        for (String selector : ROOM_TABLE_SELECTORS) {
            try {
                Locator table = page.locator(selector);
                if (table.count() == 0) {
                    continue;
                }

                List<RoomOption> options = parseRoomRows(table.first().locator("tr"));
                if (!options.isEmpty()) {
                    return options;
                }
            } catch (Exception ignored) {
                // Room table markup changes across Booking variants.
            }
        }

        return new ArrayList<>();
    }

    private static void waitForRoomSection(Page page) {
        for (String selector : ROOM_TABLE_SELECTORS) {
            try {
                page.locator(selector).first().scrollIntoViewIfNeeded(new Locator.ScrollIntoViewIfNeededOptions().setTimeout(1500));
                page.waitForSelector(selector, new Page.WaitForSelectorOptions().setTimeout(3000));
                return;
            } catch (Exception ignored) {
                // Try the next known room table selector.
            }
        }
    }

    private static List<RoomOption> parseRoomRows(Locator rows) {
        List<RoomOption> options = new ArrayList<>();
        String previousRoomType = null;
        String previousRoomSize = null;
        String previousBedInfo = null;
        List<String> previousHighlights = new ArrayList<>();
        List<String> previousAmenities = new ArrayList<>();

        int rowCount = rows.count();
        for (int index = 0; index < rowCount; index++) {
            Locator row = rows.nth(index);
            String rowText = extractText(row);
            if (!isBookableRoomRow(row, rowText)) {
                continue;
            }

            Locator cells = row.locator("td");
            int cellCount = cells.count();
            if (cellCount < 3) {
                continue;
            }

            Locator roomCell = cellCount >= 5 ? cells.nth(0) : null;
            Locator guestCell = cellCount >= 5 ? cells.nth(1) : cells.nth(0);
            Locator priceCell = cellCount >= 5 ? cells.nth(2) : cells.nth(1);
            Locator choicesCell = cellCount >= 5 ? cells.nth(3) : cells.nth(2);

            RoomOption option = new RoomOption();
            option.setRoomType(previousRoomType);
            option.setRoomSize(previousRoomSize);
            option.setBedInfo(previousBedInfo);
            option.getHighlights().addAll(previousHighlights);
            option.getAmenities().addAll(previousAmenities);

            if (roomCell != null) {
                RoomOption baseRoomData = parseRoomCell(roomCell);
                mergeRoomData(option, baseRoomData);
                if (!"N/A".equals(baseRoomData.getRoomType())) {
                    previousRoomType = baseRoomData.getRoomType();
                    previousRoomSize = baseRoomData.getRoomSize();
                    previousBedInfo = baseRoomData.getBedInfo();
                    previousHighlights = new ArrayList<>(baseRoomData.getHighlights());
                    previousAmenities = new ArrayList<>(baseRoomData.getAmenities());
                }
            }

            option.setGuests(extractGuestInfo(guestCell));
            option.setPrice(extractRoomPrice(priceCell));
            option.setTaxesAndFees(extractTaxes(priceCell));
            option.getPolicies().addAll(extractPolicies(choicesCell));
            option.setAvailability(extractAvailability(row, roomCell));

            if (isUsefulRoomOption(option)) {
                options.add(option);
            }
        }

        return options;
    }

    private static RoomOption parseRoomCell(Locator roomCell) {
        RoomOption option = new RoomOption();
        List<String> lines = splitLineValues(extractText(roomCell));
        option.setRoomType(firstNonBlank(
                extractText(roomCell, "a"),
                extractText(roomCell, "span"),
                findFirstMatching(lines, null),
                "N/A"
        ));
        option.setRoomSize(firstNonBlank(findFirstMatching(lines, ROOM_SIZE_PATTERN), "N/A"));
        option.setBedInfo(firstNonBlank(findFirstMatching(lines, BED_PATTERN), "N/A"));

        for (String line : lines) {
            if (line.equals(option.getRoomType()) || line.equals(option.getRoomSize()) || line.equals(option.getBedInfo())) {
                continue;
            }
            if (isNoise(line)) {
                continue;
            }
            if (looksLikeHighlight(line)) {
                option.getHighlights().add(line);
            } else {
                option.getAmenities().add(line);
            }
        }

        deduplicate(option.getHighlights());
        deduplicate(option.getAmenities());
        return option;
    }

    private static void mergeRoomData(RoomOption target, RoomOption source) {
        target.setRoomType(source.getRoomType());
        target.setRoomSize(source.getRoomSize());
        target.setBedInfo(source.getBedInfo());
        target.getHighlights().clear();
        target.getHighlights().addAll(source.getHighlights());
        target.getAmenities().clear();
        target.getAmenities().addAll(source.getAmenities());
    }

    private static String extractGuestInfo(Locator guestCell) {
        String guestText = normalizeText(extractText(guestCell));
        if (guestText != null) {
            return guestText;
        }

        int iconCount = 0;
        try {
            iconCount = guestCell.locator("svg, i, span[class*='occupancy'], span[class*='guest']").count();
        } catch (Exception ignored) {
            // Occupancy may be icon-only.
        }

        return iconCount > 0 ? iconCount + " guest(s)" : null;
    }

    private static String extractRoomPrice(Locator priceCell) {
        List<String> lines = splitLineValues(extractText(priceCell));
        String price = null;
        for (String line : lines) {
            if (!PRICE_PATTERN.matcher(line).matches()) {
                continue;
            }
            if (TAX_PATTERN.matcher(line).matches()) {
                continue;
            }
            if (!isValidDisplayPrice(line)) {
                continue;
            }
            price = line;
            break;
        }
        return firstNonBlank(price, "N/A");
    }

    private static String extractTaxes(Locator priceCell) {
        List<String> lines = splitLineValues(extractText(priceCell));
        return firstNonBlank(findFirstMatching(lines, TAX_PATTERN), null);
    }

    private static List<String> extractPolicies(Locator choicesCell) {
        List<String> policies = new ArrayList<>();
        for (String line : splitLineValues(extractText(choicesCell))) {
            if (!isNoise(line)) {
                policies.add(line);
            }
        }
        deduplicate(policies);
        return policies;
    }

    private static String extractAvailability(Locator row, Locator roomCell) {
        String roomText = roomCell == null ? null : extractText(roomCell);
        for (String line : splitLineValues(firstNonBlank(roomText, extractText(row)))) {
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.contains("left") || lower.contains("sold out") || lower.contains("only")) {
                return line;
            }
        }
        return "N/A";
    }

    private static boolean isBookableRoomRow(Locator row, String rowText) {
        if (rowText == null) {
            return false;
        }

        String lower = rowText.toLowerCase(Locale.ROOT);
        boolean hasPrice = PRICE_PATTERN.matcher(rowText).matches();
        boolean hasBookingControl = lower.contains("reserve") || lower.contains("select rooms") || lower.contains("free cancellation")
                || lower.contains("prepayment") || lower.contains("credit card needed") || lower.contains("property");

        if (hasPrice && hasBookingControl) {
            return true;
        }

        try {
            return row.locator("select, button, input[type='number']").count() > 0 && hasPrice;
        } catch (Exception ignored) {
            // If controls cannot be inspected, treat the row as not bookable.
            return false;
        }
    }

    private static boolean isUsefulRoomOption(RoomOption option) {
        return !"N/A".equals(option.getPrice()) || !"N/A".equals(option.getRoomType());
    }

    private static boolean looksLikeAddress(String candidate) {
        if (candidate == null) {
            return false;
        }
        String lower = candidate.toLowerCase(Locale.ROOT);
        return containsAny(lower, "street", " road", " avenue", " lane", " boulevard", " drive", "india", "delhi",
                "mumbai", "bengaluru", "bangalore", "hyderabad", "chennai", "pune", "kolkata");
    }

    private static boolean containsAny(String value, String... tokens) {
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static boolean looksLikeHighlight(String line) {
        String lower = line.toLowerCase(Locale.ROOT);
        return containsAny(lower, "view", "bathroom", "tv", "wifi", "air conditioning", "soundproof", "balcony");
    }

    private static boolean isNoise(String line) {
        if (line == null) {
            return true;
        }
        String lower = line.toLowerCase(Locale.ROOT);
        return lower.equals("more") || lower.equals("select rooms") || lower.equals("i'll reserve") || lower.equals("reserve")
                || lower.equals("your choices") || lower.equals("today's price");
    }

    private static List<String> splitLineValues(String text) {
        List<String> lines = new ArrayList<>();
        if (text == null) {
            return lines;
        }
        for (String line : text.split("\\r?\\n")) {
            String normalized = normalizeText(line);
            if (normalized != null) {
                lines.add(normalized);
            }
        }
        return lines;
    }

    private static String findFirstMatching(List<String> lines, Pattern pattern) {
        for (String line : lines) {
            if (pattern == null && !isNoise(line)) {
                return line;
            }
            if (pattern != null && pattern.matcher(line).matches()) {
                return line;
            }
        }
        return null;
    }

    private static void deduplicate(List<String> values) {
        Set<String> unique = new LinkedHashSet<>(values);
        values.clear();
        values.addAll(unique);
    }

    private static boolean isValidDisplayPrice(String priceText) {
        if (priceText == null) {
            return false;
        }
        String lower = priceText.toLowerCase(Locale.ROOT);
        if (!PRICE_PATTERN.matcher(priceText).matches()) {
            return false;
        }
        if (lower.contains("per person") || lower.contains("person, per") || lower.contains("/ person")) {
            return false;
        }
        return !lower.contains("tax") && !lower.contains("fee") && !lower.startsWith("+");
    }

    private static Integer parsePriceAmount(String priceText) {
        if (priceText == null) {
            return null;
        }

        Matcher matcher = Pattern.compile("(\\d[\\d,]*)").matcher(priceText);
        if (!matcher.find()) {
            return null;
        }

        try {
            return Integer.parseInt(matcher.group(1).replace(",", ""));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static double similarityScore(String requestedName, String candidateName) {
        Set<String> requestedTokens = tokenSet(requestedName);
        Set<String> candidateTokens = tokenSet(candidateName);
        if (requestedTokens.isEmpty() || candidateTokens.isEmpty()) {
            return 0;
        }

        int overlap = 0;
        for (String token : requestedTokens) {
            if (candidateTokens.contains(token)) {
                overlap++;
            }
        }
        return overlap * 100.0 / requestedTokens.size();
    }

    private static Set<String> tokenSet(String value) {
        Set<String> tokens = new LinkedHashSet<>();
        String normalized = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]", " ");
        for (String token : normalized.split("\\s+")) {
            if (!token.trim().isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private static String normalizeBookingUrl(String href) {
        if (href == null || href.trim().isEmpty()) {
            return null;
        }
        return href.startsWith("http") ? href : BOOKING_BASE_URL + href;
    }

    private static String buildSearchUrl(String hotelName) {
        LocalDate checkIn = LocalDate.now().plusDays(DATE_RETRY_OFFSETS[0]);
        LocalDate checkOut = checkIn.plusDays(DEFAULT_STAY_NIGHTS);
        return BOOKING_BASE_URL + "/searchresults.html?ss=" + urlEncode(hotelName)
                + "&checkin=" + checkIn
                + "&checkout=" + checkOut
                + "&group_adults=2&no_rooms=1&group_children=0&lang=en-us";
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalStateException("UTF-8 encoding must be available", exception);
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = normalizeText(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private static String normalizeText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        // Remove Hindi/Devanagari characters and clean up the text
        String cleaned = text.replaceAll("[\\u0900-\\u097F]", "").trim();
        // Remove extra spaces and commas
        cleaned = cleaned.replaceAll("\\s+", " ").replaceAll(",\\s*,", ",").replaceAll("^,|,$", "");
        return cleaned.isEmpty() ? null : cleaned;
    }

    private static String cleanHindiText(String text) {
        if (text == null) return null;
        // Split by comma and take the first English part, or remove Hindi characters
        String[] parts = text.split(",");
        for (String part : parts) {
            part = part.trim();
            if (!part.matches(".*[\\u0900-\\u097F].*")) {
                return part;
            }
        }
        // If no clean part found, remove all Hindi characters
        return text.replaceAll("[\\u0900-\\u097F]", "").replaceAll("\\s+", " ").trim();
    }

    private static String joinNonBlank(String delimiter, String... values) {
        List<String> parts = new ArrayList<>();
        for (String value : values) {
            String normalized = normalizeText(value);
            if (normalized != null) {
                parts.add(normalized);
            }
        }
        if (parts.isEmpty()) {
            return null;
        }
        return String.join(delimiter, parts);
    }

    private static String unescapeJson(String value) {
        return value
                .replace("\\/", "/")
                .replace("\\\"", "\"")
                .replace("\\n", " ")
                .replace("\\u0026", "&");
    }
}
