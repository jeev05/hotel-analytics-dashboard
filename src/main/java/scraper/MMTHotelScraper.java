package scraper;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
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
import model.Hotel;
import model.RoomOption;

public class MMTHotelScraper {
    private static final Logger LOGGER = Logger.getLogger(MMTHotelScraper.class.getName());
    private static final String MMT_BASE_URL = "https://www.makemytrip.com";
    private static final String[] RESULT_CARD_SELECTORS = {
            "[data-testid='hotelCard']",
            "div[class*='listingRow']",
            "div[class*='hotelListingCard']",
            "a[href*='/hotel/']",
            "a[href*='hotel-details']",
            "a[href*='hotelid']",
            "a[href*='hotels/']"
        };
        private static final String[] SEARCH_INPUT_SELECTORS = {
            "input[id='city']",
            "input[placeholder*='City']",
            "input[placeholder*='locality']",
            "input[type='text']"
        };
        private static final String[] SEARCH_BUTTON_SELECTORS = {
            "button[data-cy='submit']",
            "button:has-text('Search')",
            "button[type='submit']"
        };
        private static final String[] RATING_SELECTORS = {
            "[data-testid='ratingValue']",
            "span[class*='rating']",
            "span[class*='latoBlack']"
        };
        private static final String[] REVIEW_SELECTORS = {
            "[data-testid='reviewCount']",
            "span[class*='review']",
            "span[class*='reviewCount']"
        };
        private static final String[] PRICE_SELECTORS = {
            "[data-testid='roomPrice']",
            "[data-testid='priceValue']",
            "span[class*='price']",
            "div[class*='price']"
    };
    private static final String[] ROOM_CARD_SELECTORS = {
            "[data-testid='roomCard']",
            "div[class*='roomCardWrap']",
            "div[class*='roomWrap']",
            "div[class*='roomRow']"
    };
    private static final int SEARCH_TIMEOUT_MS = 20000;
    private static final int MAX_RESULTS_TO_SCORE = 5;
    private static final int MAX_AMENITIES = 12;
    private static final int DEFAULT_STAY_NIGHTS = 1;
    private static final Pattern PRICE_PATTERN = Pattern.compile(".*[₹$€£].*\\d.*");
    private static final Pattern REVIEW_PATTERN = Pattern.compile("(\\d[\\d,]*)");
    private static final Pattern RATING_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)");
    private static final Pattern ROOM_SIZE_PATTERN = Pattern.compile(".*(sq\\.? ?ft|m²|square).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern BED_PATTERN = Pattern.compile(".*\\b(bed|beds)\\b.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern TAX_PATTERN = Pattern.compile(".*(tax|fee).*", Pattern.CASE_INSENSITIVE);
    private static final String BLOCKED_PAGE_MARKER = "200-OK";
    private static final Pattern JSON_STRING_FIELD_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern JSON_NUMBER_FIELD_PATTERN = Pattern.compile("\"%s\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");

    public static void main(String[] args) {
        configureLogging();

        String hotelName = promptForHotelName();
        if (hotelName == null) {
            return;
        }

        Hotel hotel = scrapeHotelByName(hotelName);
        if (hotel == null) {
            LOGGER.severe("Could not extract hotel details from MakeMyTrip.");
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

    private static Hotel scrapeHotelByName(String hotelName) {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                 .setHeadless(false)
                 .setArgs(Arrays.asList("--disable-blink-features=AutomationControlled")));
             BrowserContext context = browser.newContext(
                 new Browser.NewContextOptions()
                     .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                     .setLocale("en-IN")
                     .setTimezoneId("Asia/Kolkata"));
             Page page = context.newPage()) {
            applyStealthScript(context);

            LOGGER.log(Level.INFO, "Searching for hotel: {0} on MakeMyTrip...", hotelName);
            String propertyUrl = navigateToBestMatch(page, hotelName);
            if (propertyUrl == null) {
                return null;
            }

            dismissPopups(page);
            page.waitForTimeout(4000);
            return extractHotelDetails(page, hotelName, propertyUrl);
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Error during MMT scraping", exception);
            return null;
        }
    }

    private static String navigateToBestMatch(Page page, String hotelName) {
        boolean searchLoaded = loadSearchResults(page, hotelName);
        if (!searchLoaded) {
            LOGGER.log(Level.WARNING, "Could not load MMT search results for: {0}", hotelName);
            String fallbackUrl = findBestUrlViaGoogle(page, hotelName);
            if (fallbackUrl != null) {
                page.navigate(fallbackUrl);
                return fallbackUrl;
            }
            return null;
        }

        Locator cards = locateFirstExisting(page, RESULT_CARD_SELECTORS);
        if (cards == null) {
            LOGGER.warning("MMT search results container was not detected. Falling back to link scan.");
            String fallbackUrl = findBestUrlFromPageLinks(page, hotelName);
            if (fallbackUrl != null) {
                page.navigate(fallbackUrl);
                return fallbackUrl;
            }
            String googleUrl = findBestUrlViaGoogle(page, hotelName);
            if (googleUrl != null) {
                page.navigate(googleUrl);
                return googleUrl;
            }
            return null;
        }

        int hotelCount = cards.count();
        LOGGER.log(Level.INFO, "Hotels Found: {0}", hotelCount);
        if (hotelCount == 0) {
            LOGGER.log(Level.WARNING, "No hotels found matching: {0}", hotelName);
            String googleUrl = findBestUrlViaGoogle(page, hotelName);
            if (googleUrl != null) {
                page.navigate(googleUrl);
                return googleUrl;
            }
            return null;
        }

        String bestUrl = null;
        double bestScore = -1;

        for (int index = 0; index < Math.min(hotelCount, MAX_RESULTS_TO_SCORE); index++) {
            Locator card = cards.nth(index);
            String title = firstNonBlank(
                    extractText(card, "[data-testid='hotelName']"),
                    extractText(card, "p[class*='wordBreak']"),
                    extractText(card, "a")
            );

            String href = firstNonBlank(
                    extractAttribute(card, "a", "href"),
                    extractAttribute(card, "[href]", "href")
            );

            if (title == null || href == null) {
                continue;
            }

            String normalizedHref = normalizeMmtUrl(href);
            if (!isLikelyHotelDetailUrl(normalizedHref)) {
                continue;
            }

            double score = similarityScore(hotelName, title);
            if (score > bestScore) {
                bestScore = score;
                bestUrl = normalizedHref;
            }
        }

        if (bestUrl == null) {
            bestUrl = findBestUrlFromPageLinks(page, hotelName);
            if (bestUrl == null) {
                bestUrl = findBestUrlViaGoogle(page, hotelName);
            }
            if (bestUrl == null) {
                return null;
            }
        }

        LOGGER.log(Level.INFO, "Opening best matching MMT hotel... score={0}", Math.round(bestScore));
        page.navigate(bestUrl);
        return bestUrl;
    }

    private static boolean loadSearchResults(Page page, String hotelName) {
        try {
            page.navigate(MMT_BASE_URL + "/hotels/");
            dismissPopups(page);
            if (isBlockedPage(page)) {
                return false;
            }
            if (submitSearchFromHomepage(page, hotelName)) {
                if (!waitForAnySelector(page, RESULT_CARD_SELECTORS, SEARCH_TIMEOUT_MS)) {
                    return false;
                }
                page.waitForTimeout(2500);
                return true;
            }
        } catch (Exception homepageSearchException) {
            LOGGER.log(Level.FINE, "Homepage MMT search failed, falling back to URL search.", homepageSearchException);
        }

        try {
            page.navigate(buildSearchUrl(hotelName));
            dismissPopups(page);
            if (isBlockedPage(page)) {
                return false;
            }
            if (!waitForAnySelector(page, RESULT_CARD_SELECTORS, SEARCH_TIMEOUT_MS)) {
                return false;
            }
            page.waitForTimeout(2500);
            return true;
        } catch (Exception urlSearchException) {
            LOGGER.log(Level.FINE, "URL MMT search failed.", urlSearchException);
            return false;
        }
    }

    private static boolean submitSearchFromHomepage(Page page, String hotelName) {
        waitForAnySelector(page, SEARCH_INPUT_SELECTORS, 10000);
        Locator searchInput = locateFirstVisible(page, SEARCH_INPUT_SELECTORS);
        if (searchInput == null) {
            return false;
        }

        searchInput.click();
        searchInput.fill("");
        searchInput.fill(hotelName);
        page.waitForTimeout(1200);

        Locator suggestion = locateFirstVisible(page,
                new String[]{
                        "li[role='option']",
                        "[data-cy='suggestion-item']",
                        "[class*='react-autosuggest__suggestion']"
                });
        if (suggestion != null) {
            suggestion.click();
        }

        Locator searchButton = locateFirstVisible(page, SEARCH_BUTTON_SELECTORS);
        if (searchButton != null) {
            searchButton.click();
        } else {
            searchInput.press("Enter");
        }

        return true;
    }

    private static Hotel extractHotelDetails(Page page, String hotelName, String propertyUrl) {
        Hotel hotel = new Hotel();
        hotel.setSearchQuery(hotelName);
        hotel.setSourceUrl(firstNonBlank(page.url(), propertyUrl, "N/A"));

        List<String> jsonLdBlocks = page.locator("script[type='application/ld+json']").allTextContents();
        String scriptBlob = collectScriptBlob(page);

        hotel.setName(firstNonBlank(
                extractJsonLdField(jsonLdBlocks, "name"),
                extractText(page, "h1"),
                extractText(page, "[data-testid='hotelName']"),
            extractScriptString(scriptBlob, "hotelName", "propertyName", "name"),
                "N/A"
        ));

        hotel.setAddress(firstNonBlank(
                extractAddressFromJsonLd(jsonLdBlocks),
                extractText(page, "[data-testid='hotelAddress']"),
                extractText(page, "div[class*='addrCont']"),
                extractText(page, "p[class*='address']"),
            extractScriptString(scriptBlob, "hotelAddress", "address", "fullAddress"),
                "N/A"
        ));

        hotel.setRating(firstNonBlank(
                extractJsonLdField(jsonLdBlocks, "ratingValue"),
            extractRating(page),
            extractScriptNumber(scriptBlob, "rating", "starRating", "ratingValue"),
                "N/A"
        ));

        hotel.setReviews(firstNonBlank(
                extractJsonLdField(jsonLdBlocks, "reviewCount"),
            extractReviewCount(page),
            extractScriptNumber(scriptBlob, "reviewCount", "totalReviews", "reviewCnt"),
                "N/A"
        ));

        hotel.setDescription(firstNonBlank(
                extractMetaContent(page, "meta[name='description']"),
                extractText(page, "div[class*='hotelDescription']"),
            extractScriptString(scriptBlob, "description", "hotelDescription"),
                "N/A"
        ));

        List<String> amenities = extractAmenities(page);
        if (amenities.isEmpty()) {
            amenities = extractAmenitiesFromScript(scriptBlob);
        }
        hotel.setAmenities(amenities);

        List<RoomOption> roomOptions = extractRoomOptions(page);
        if (roomOptions.isEmpty()) {
            roomOptions = extractRoomOptionsFromScript(scriptBlob);
        }
        hotel.setRoomOptions(roomOptions);
        hotel.setPrice(resolveDisplayPrice(hotel.getRoomOptions(), page, jsonLdBlocks));
        return hotel;
    }

    private static String resolveDisplayPrice(List<RoomOption> roomOptions, Page page, List<String> jsonLdBlocks) {
        String fromRooms = pickRepresentativeRoomPrice(roomOptions);
        if (fromRooms != null) {
            return fromRooms;
        }

        String jsonPrice = extractJsonLdField(jsonLdBlocks, "priceRange");
        if (isValidDisplayPrice(jsonPrice)) {
            return jsonPrice;
        }

        String fromPage = extractFirstPrice(page);
        return firstNonBlank(fromPage, "N/A");
    }

    private static String pickRepresentativeRoomPrice(List<RoomOption> roomOptions) {
        String bestPrice = null;
        int bestValue = Integer.MAX_VALUE;
        for (RoomOption option : roomOptions) {
            String price = normalizeText(option.getPrice());
            Integer numeric = parsePriceAmount(price);
            if (!isValidDisplayPrice(price) || numeric == null) {
                continue;
            }
            if (numeric < bestValue) {
                bestValue = numeric;
                bestPrice = price;
            }
        }
        return bestPrice;
    }

    private static List<String> extractAmenities(Page page) {
        Set<String> amenities = new LinkedHashSet<>();
        collectAmenities(page, amenities, "[data-testid='amenityItem']");
        collectAmenities(page, amenities, "li[class*='amenity']");
        collectAmenities(page, amenities, "div[class*='amenityItem']");
        collectAmenities(page, amenities, "[class*='facilities'] li");
        return new ArrayList<>(amenities);
    }

    private static void collectAmenities(Page page, Set<String> amenities, String selector) {
        try {
            for (String text : page.locator(selector).allTextContents()) {
                String normalized = normalizeText(text);
                if (normalized != null && normalized.length() <= 80) {
                    amenities.add(normalized);
                }
                if (amenities.size() >= MAX_AMENITIES) {
                    return;
                }
            }
        } catch (Exception ignored) {
            // Continue with alternate amenity selectors.
        }
    }

    private static List<RoomOption> extractRoomOptions(Page page) {
        scrollToRoomSection(page);

        for (String selector : ROOM_CARD_SELECTORS) {
            try {
                Locator cards = page.locator(selector);
                if (cards.count() == 0) {
                    continue;
                }

                List<RoomOption> options = new ArrayList<>();
                int cardCount = cards.count();
                for (int index = 0; index < cardCount; index++) {
                    RoomOption option = parseRoomCard(cards.nth(index));
                    if (isUsefulRoomOption(option)) {
                        options.add(option);
                    }
                }

                if (!options.isEmpty()) {
                    return options;
                }
            } catch (Exception ignored) {
                // MMT room card markup varies by property.
            }
        }

        return new ArrayList<>();
    }

    private static List<RoomOption> extractRoomOptionsFromScript(String scriptBlob) {
        List<RoomOption> options = new ArrayList<>();
        if (scriptBlob == null || scriptBlob.isBlank()) {
            return options;
        }

        Pattern roomNamePattern = Pattern.compile("\\\"(roomTypeName|roomName|name)\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
        Pattern pricePattern = Pattern.compile("\\\"(finalPrice|displayPrice|price|roomPrice)\\\"\\s*:\\s*\\\"?([0-9,]{3,})\\\"?");
        Pattern policyPattern = Pattern.compile("\\\"(cancellationPolicy|policyText|bookingPolicy)\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");

        Matcher nameMatcher = roomNamePattern.matcher(scriptBlob);
        Matcher priceMatcher = pricePattern.matcher(scriptBlob);
        Matcher policyMatcher = policyPattern.matcher(scriptBlob);

        List<String> names = new ArrayList<>();
        List<String> prices = new ArrayList<>();
        List<String> policies = new ArrayList<>();

        while (nameMatcher.find() && names.size() < 8) {
            String value = normalizeText(unescapeJson(nameMatcher.group(2)));
            if (value != null && isPotentialRoomType(value)) {
                names.add(value);
            }
        }
        while (priceMatcher.find() && prices.size() < 8) {
            String value = "Rs. " + priceMatcher.group(2).replace(",", "");
            if (isValidDisplayPrice(value)) {
                prices.add(value);
            }
        }
        while (policyMatcher.find() && policies.size() < 12) {
            String value = normalizeText(unescapeJson(policyMatcher.group(2)));
            if (value != null && !isNoise(value)) {
                policies.add(value);
            }
        }

        int count = Math.min(Math.max(names.size(), prices.size()), 6);
        for (int index = 0; index < count; index++) {
            RoomOption option = new RoomOption();
            option.setRoomType(index < names.size() ? names.get(index) : "Room Option " + (index + 1));
            option.setPrice(index < prices.size() ? prices.get(index) : "N/A");
            option.setTaxesAndFees("N/A");
            option.setGuests("N/A");
            option.setRoomSize("N/A");
            option.setBedInfo("N/A");
            option.setAvailability("N/A");
            if (!policies.isEmpty()) {
                option.getPolicies().add(policies.get(index % policies.size()));
            }
            options.add(option);
        }

        return options;
    }

    private static RoomOption parseRoomCard(Locator card) {
        RoomOption option = new RoomOption();
        String fullText = extractText(card);
        List<String> lines = splitLineValues(fullText);

        option.setRoomType(firstNonBlank(
                extractText(card, "[data-testid='roomName']"),
            extractText(card, "span[class*='roomName']"),
                extractText(card, "h3"),
                findFirstMatching(lines, null),
                "N/A"
        ));
        option.setPrice(firstNonBlank(
                extractText(card, "[data-testid='roomPrice']"),
                extractText(card, "span[class*='price']"),
                findFirstMatching(lines, PRICE_PATTERN),
                "N/A"
        ));
        option.setTaxesAndFees(firstNonBlank(findFirstMatching(lines, TAX_PATTERN), "N/A"));
        option.setRoomSize(firstNonBlank(findFirstMatching(lines, ROOM_SIZE_PATTERN), "N/A"));
        option.setBedInfo(firstNonBlank(findFirstMatching(lines, BED_PATTERN), "N/A"));
        option.setGuests(firstNonBlank(extractGuestInfo(card), "N/A"));
        option.setAvailability(firstNonBlank(findAvailability(lines), "N/A"));
        option.getPolicies().addAll(extractPolicies(lines));

        for (String line : lines) {
            if (shouldSkipRoomLine(line, option)) {
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
        deduplicate(option.getPolicies());
        return option;
    }

    private static String extractGuestInfo(Locator card) {
        String guestText = firstNonBlank(
                extractText(card, "[data-testid='guestCount']"),
                extractText(card, "span[class*='guest']")
        );
        if (guestText != null) {
            return guestText;
        }

        try {
            int icons = card.locator("svg, i, span[class*='guestIcon']").count();
            return icons > 0 ? icons + " guest(s)" : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String findAvailability(List<String> lines) {
        for (String line : lines) {
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.contains("left") || lower.contains("available") || lower.contains("sold out")) {
                return line;
            }
        }
        return null;
    }

    private static List<String> extractPolicies(List<String> lines) {
        List<String> policies = new ArrayList<>();
        for (String line : lines) {
            String lower = line.toLowerCase(Locale.ROOT);
            if (containsAny(lower, "free cancellation", "breakfast", "non-refundable", "pay at hotel", "book now")) {
                policies.add(line);
            }
        }
        return policies;
    }

    private static List<String> extractAmenitiesFromScript(String scriptBlob) {
        List<String> amenities = new ArrayList<>();
        if (scriptBlob == null || scriptBlob.isBlank()) {
            return amenities;
        }

        Pattern amenityPattern = Pattern.compile("\\\"(amenityName|facilityName|amenity|facility)\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
        Matcher matcher = amenityPattern.matcher(scriptBlob);
        while (matcher.find() && amenities.size() < MAX_AMENITIES) {
            String value = normalizeText(unescapeJson(matcher.group(2)));
            if (value == null || isNoise(value)) {
                continue;
            }
            if (!amenities.contains(value)) {
                amenities.add(value);
            }
        }
        return amenities;
    }

    private static boolean shouldSkipRoomLine(String line, RoomOption option) {
        if (line == null) {
            return true;
        }
        return line.equals(option.getRoomType())
                || line.equals(option.getPrice())
                || line.equals(option.getTaxesAndFees())
                || line.equals(option.getRoomSize())
                || line.equals(option.getBedInfo())
                || line.equals(option.getGuests())
                || line.equals(option.getAvailability())
                || isNoise(line);
    }

    private static boolean isUsefulRoomOption(RoomOption option) {
        return !"N/A".equals(option.getRoomType()) || !"N/A".equals(option.getPrice());
    }

    private static boolean isPotentialRoomType(String value) {
        if (value == null) {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (value.length() > 90) {
            return false;
        }
        if (containsAny(lower, "check-in", "checkout", "search", "apply dates", "rooms & guests", "tooltip")) {
            return false;
        }
        return containsAny(lower, "room", "suite", "deluxe", "king", "twin", "premium", "executive", "studio");
    }

    private static String extractFirstPrice(Page page) {
        List<String> candidates = new ArrayList<>();
        for (String selector : PRICE_SELECTORS) {
            candidates.add(extractText(page, selector));
        }
        for (String candidate : candidates) {
            String parsed = firstValidPriceFromText(candidate);
            if (parsed != null) {
                return parsed;
            }
        }

        try {
            for (String text : page.locator("span, div").allTextContents()) {
                String parsed = firstValidPriceFromText(text);
                if (parsed != null) {
                    return parsed;
                }
            }
        } catch (Exception ignored) {
            // Continue with other fields if price is not visible.
        }
        return null;
    }

    private static String extractRating(Page page) {
        for (String selector : RATING_SELECTORS) {
            String rating = extractNumeric(extractText(page, selector), RATING_PATTERN);
            if (rating != null) {
                return rating;
            }
        }
        return null;
    }

    private static String extractReviewCount(Page page) {
        for (String selector : REVIEW_SELECTORS) {
            String reviews = extractNumeric(extractText(page, selector), REVIEW_PATTERN);
            if (reviews != null) {
                return reviews;
            }
        }
        return null;
    }

    private static void scrollToRoomSection(Page page) {
        try {
            for (int index = 0; index < 4; index++) {
                page.mouse().wheel(0, 1800);
                page.waitForTimeout(1000);
            }
        } catch (Exception ignored) {
            // Room sections are lazily loaded.
        }
    }

    private static void dismissPopups(Page page) {
        clickIfVisible(page, "span[data-cy='closeModal']");
        clickIfVisible(page, "button[data-cy='closeModal']");
        clickIfVisible(page, "span[class*='close']");
        clickIfVisible(page, "button:has-text('Got it')");
        clickIfVisible(page, "button:has-text('Accept')");
    }

    private static void clickIfVisible(Page page, String selector) {
        try {
            Locator locator = page.locator(selector);
            if (locator.count() > 0 && locator.first().isVisible()) {
                locator.first().click(new Locator.ClickOptions().setTimeout(1500));
            }
        } catch (Exception ignored) {
            // Optional popup may not be visible.
        }
    }

    private static boolean waitForAnySelector(Page page, String[] selectors, int timeoutMs) {
        for (String selector : selectors) {
            try {
                page.waitForSelector(selector, new Page.WaitForSelectorOptions().setTimeout(timeoutMs));
                return true;
            } catch (Exception ignored) {
                // Try next selector.
            }
        }
        return false;
    }

    private static String findBestUrlFromPageLinks(Page page, String hotelName) {
        try {
            String bestUrl = null;
            double bestScore = -1;
            Locator links = page.locator("a[href]");
            int linkCount = links.count();

            for (int index = 0; index < linkCount; index++) {
                String rawHref = links.nth(index).getAttribute("href");
                String normalized = normalizeMmtUrl(rawHref);
                if (!isLikelyHotelDetailUrl(normalized)) {
                    continue;
                }

                String linkText = normalizeText(extractText(links.nth(index)));
                String combined = firstNonBlank(linkText, "") + " " + normalized;

                double score = similarityScore(hotelName, combined);
                if (score > bestScore) {
                    bestScore = score;
                    bestUrl = normalized;
                }
            }

            return bestScore >= 30 ? bestUrl : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String findBestUrlViaGoogle(Page page, String hotelName) {
        try {
            String query = "site:makemytrip.com hotel " + hotelName;
            page.navigate("https://www.google.com/search?q=" + urlEncode(query));
            page.waitForSelector("a[href]", new Page.WaitForSelectorOptions().setTimeout(12000));

            Locator links = page.locator("a[href]");
            int linkCount = links.count();
            String bestUrl = null;
            double bestScore = -1;

            for (int index = 0; index < linkCount; index++) {
                String href = links.nth(index).getAttribute("href");
                String normalized = normalizeGoogleOrDirectUrl(href);
                if (!isLikelyHotelDetailUrl(normalized)) {
                    continue;
                }

                String linkText = normalizeText(extractText(links.nth(index)));
                String combined = firstNonBlank(linkText, "") + " " + normalized;
                double score = similarityScore(hotelName, combined);
                if (score > bestScore) {
                    bestScore = score;
                    bestUrl = normalized;
                }
            }

            if (bestScore >= 20) {
                LOGGER.log(Level.INFO, "Google fallback found MMT property URL with score={0}", Math.round(bestScore));
                return bestUrl;
            }
            return null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String normalizeGoogleOrDirectUrl(String href) {
        if (href == null) {
            return null;
        }
        String value = href;
        if (value.startsWith("/url?q=")) {
            int start = value.indexOf("/url?q=") + 7;
            int end = value.indexOf('&', start);
            String encoded = end > start ? value.substring(start, end) : value.substring(start);
            try {
                value = URLDecoder.decode(encoded, StandardCharsets.UTF_8.name());
            } catch (Exception ignored) {
                value = encoded;
            }
        }
        return normalizeMmtUrl(value);
    }

    private static boolean isBlockedPage(Page page) {
        String bodyText = normalizeText(extractText(page, "body"));
        String title = normalizeText(page.title());
        if (bodyText == null) {
            return false;
        }
        return BLOCKED_PAGE_MARKER.equalsIgnoreCase(bodyText)
                || (title != null && title.toLowerCase(Locale.ROOT).contains("pretty"));
    }

    private static void applyStealthScript(BrowserContext context) {
        context.addInitScript("Object.defineProperty(navigator, 'webdriver', {get: () => undefined});");
        context.addInitScript("Object.defineProperty(navigator, 'platform', {get: () => 'Win32'});");
    }

    private static boolean isLikelyHotelDetailUrl(String url) {
        if (url == null) {
            return false;
        }
        String lower = url.toLowerCase(Locale.ROOT);
        if (!lower.contains("makemytrip.com")) {
            return false;
        }
        return lower.contains("hotel-details") || lower.contains("/hotel/") || lower.contains("hotelid=");
    }

    private static Locator locateFirstExisting(Page page, String[] selectors) {
        for (String selector : selectors) {
            Locator locator = page.locator(selector);
            if (locator.count() > 0) {
                return locator;
            }
        }
        return null;
    }

    private static Locator locateFirstVisible(Page page, String[] selectors) {
        for (String selector : selectors) {
            try {
                Locator locator = page.locator(selector);
                if (locator.count() > 0 && locator.first().isVisible()) {
                    return locator.first();
                }
            } catch (Exception ignored) {
                // Keep trying alternate selectors.
            }
        }
        return null;
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
            return null;
        }
    }

    private static String extractJsonLdField(List<String> jsonLdBlocks, String fieldName) {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(fieldName) + "\\\"\\s*:\\s*\\\"(.*?)\\\"");
        for (String block : jsonLdBlocks) {
            Matcher matcher = pattern.matcher(block);
            if (matcher.find()) {
                return normalizeText(unescapeJson(matcher.group(1)));
            }
        }
        return null;
    }

    private static String collectScriptBlob(Page page) {
        try {
            List<String> scripts = page.locator("script").allTextContents();
            return String.join("\n", scripts);
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String extractScriptString(String scriptBlob, String... fieldNames) {
        if (scriptBlob == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            Pattern pattern = Pattern.compile(String.format(JSON_STRING_FIELD_PATTERN.pattern(), Pattern.quote(fieldName)));
            Matcher matcher = pattern.matcher(scriptBlob);
            if (matcher.find()) {
                return normalizeText(unescapeJson(matcher.group(1)));
            }
        }
        return null;
    }

    private static String extractScriptNumber(String scriptBlob, String... fieldNames) {
        if (scriptBlob == null) {
            return null;
        }
        for (String fieldName : fieldNames) {
            Pattern pattern = Pattern.compile(String.format(JSON_NUMBER_FIELD_PATTERN.pattern(), Pattern.quote(fieldName)));
            Matcher matcher = pattern.matcher(scriptBlob);
            if (matcher.find()) {
                return normalizeText(matcher.group(1));
            }
        }
        return null;
    }

    private static String extractAddressFromJsonLd(List<String> jsonLdBlocks) {
        for (String block : jsonLdBlocks) {
            String street = extractJsonField(block, "streetAddress");
            String locality = extractJsonField(block, "addressLocality");
            String region = extractJsonField(block, "addressRegion");
            String postalCode = extractJsonField(block, "postalCode");
            String country = extractJsonField(block, "addressCountry");
            String address = joinNonBlank(", ", street, locality, region, postalCode, country);
            if (address != null) {
                return address;
            }
        }
        return null;
    }

    private static String extractJsonField(String block, String fieldName) {
        Pattern pattern = Pattern.compile("\\\"" + Pattern.quote(fieldName) + "\\\"\\s*:\\s*\\\"(.*?)\\\"");
        Matcher matcher = pattern.matcher(block);
        if (matcher.find()) {
            return normalizeText(unescapeJson(matcher.group(1)));
        }
        return null;
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

    private static boolean looksLikeHighlight(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return containsAny(lower, "wifi", "breakfast", "view", "air conditioning", "tv", "balcony", "bathroom");
    }

    private static boolean containsAny(String value, String... tokens) {
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNoise(String value) {
        if (value == null) {
            return true;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.equals("more") || lower.equals("show more") || lower.equals("book now") || lower.equals("room details")
                || lower.equals("select room");
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

    private static String extractNumeric(String text, Pattern pattern) {
        if (text == null) {
            return null;
        }
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static String normalizeMmtUrl(String href) {
        if (href == null || href.trim().isEmpty()) {
            return null;
        }
        return href.startsWith("http") ? href : MMT_BASE_URL + href;
    }

    private static String buildSearchUrl(String hotelName) {
        LocalDate checkIn = LocalDate.now().plusDays(7);
        LocalDate checkOut = checkIn.plusDays(DEFAULT_STAY_NIGHTS);
        return MMT_BASE_URL + "/hotels/hotel-listing/?searchText=" + urlEncode(hotelName)
                + "&checkin=" + checkIn + "&checkout=" + checkOut
                + "&roomStayQualifier=2e0e";
    }

    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalStateException("UTF-8 encoding must be available", exception);
        }
    }

    private static boolean isValidDisplayPrice(String priceText) {
        if (priceText == null) {
            return false;
        }
        String normalized = normalizeText(priceText);
        if (normalized == null) {
            return false;
        }
        if (normalized.length() > 40) {
            return false;
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (!PRICE_PATTERN.matcher(normalized).matches()) {
            return false;
        }
        if (lower.contains("per person") || lower.contains("tax") || lower.contains("fee") || lower.startsWith("+")) {
            return false;
        }
        if (containsAny(lower, "check-in", "checkin", "checkout", "apply dates", "rooms & guests", "search")) {
            return false;
        }
        return normalized.matches(".*[₹$€£]\\s?[0-9,]{3,}.*");
    }

    private static String firstValidPriceFromText(String text) {
        for (String line : splitLineValues(text)) {
            if (isValidDisplayPrice(line)) {
                return line;
            }
        }
        return null;
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

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = normalizeText(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
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

    private static String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String unescapeJson(String value) {
        return value.replace("\\/", "/")
                .replace("\\\"", "\"")
                .replace("\\n", " ")
                .replace("\\u0026", "&");
    }
}