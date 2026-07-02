package scraper;


import com.hotelapi.service.ImageAnalyzer;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;


import model.CompetitorAnalysis;
import model.Hotel;
import model.RoomOption;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.nio.file.Files;


public class AgodaScraper {


    private static final Logger LOGGER =
            Logger.getLogger(AgodaScraper.class.getName());


    private static final String AGODA_BASE_URL =
            "https://www.agoda.com";


    private static final int SEARCH_TIMEOUT_MS = 45000;


    // ─────────────────────────────────────────────────────────────
    //  AMENITY SECTIONS / MINIMAL JUNK FILTER  (NO whitelist filtering)
    // ─────────────────────────────────────────────────────────────


    /** Headings under which Agoda lists hotel facilities/amenities. */
    private static final List<String> AMENITY_SECTION_HEADERS = Arrays.asList(
            "Most popular facilities", "Facilities", "Property facilities",
            "Room facilities", "Amenities"
    );

    /** Headings that mark the end of an amenities section. */
    private static final List<String> AMENITY_SECTION_END_MARKERS = Arrays.asList(
            "Reviews", "About us", "Property policies", "Select your room",
            "Top things to do", "House rules", "More about"
    );

    /**
     * Whitelist of valid amenities. Only amenities matching this list (after normalization)
     * are kept. This prevents extraction of city names, discounts, review text, etc.
     */
    private static final Set<String> AMENITY_WHITELIST = new LinkedHashSet<>(Arrays.asList(
            "WiFi", "Parking", "Free Parking", "Restaurant", "Pool", "Swimming Pool",
            "Gym", "Fitness Center", "Spa", "Breakfast", "Bar", "Room Service",
            "Airport Shuttle", "Family Rooms", "Air Conditioning", "Laundry",
            "Elevator", "Terrace", "Garden", "Pet Friendly", "24-hour Front Desk"
    ));

    /**
     * Keywords that disqualify any amenity candidate. Filters out cities, discounts,
     * review text, prices, ratings, locations, policy text, etc.
     */
    private static final Set<String> AMENITY_FORBIDDEN_KEYWORDS = new LinkedHashSet<>(Arrays.asList(
            "discount", "coupon", "review", "rating", "score", "excellent",
            "very good", "good", "location", "city", "state", "hotel",
            "property", "available", "availability", "check", "help",
            "privacy", "terms", "price", "₹", "rs", "%", "coimbatore",
            "bandipur", "kodaikanal", "bus", "park", "stand", "opens in a new tab",
            "help center", "faq", "policy", "company", "page", "view on map",
            "book now", "agoda sponsored"
    ));

    private static final List<String> ROOM_FEATURE_SECTION_HEADERS = Arrays.asList(
            "Room facilities", "Room amenities", "Room features"
    );

    private static final List<String> ROOM_FEATURE_SECTION_END_MARKERS = Arrays.asList(
            "Property policies", "Select your room", "Top things to do", "Reviews", "House rules"
    );

    private static final Set<String> POLICY_ALLOW_KEYWORDS = new LinkedHashSet<>(Arrays.asList(
            "check-in", "check out", "check-out", "cancellation", "cancel",
            "children", "child", "pets", "pet", "smoking", "smoke",
            "payment", "refund", "deposit", "damage", "security",
            "age", "passport", "id", "identification", "late check-out",
            "early check-in", "no pets", "non-refundable", "booking changes",
            "group booking", "extra bed", "guest name", "voucher"
    ));

    private static final Set<String> POLICY_DENY_KEYWORDS = new LinkedHashSet<>(Arrays.asList(
            "wifi", "parking", "pool", "gym", "studio", "restaurant", "bar",
            "breakfast", "spa", "room service", "air conditioning", "laundry",
            "elevator", "terrace", "garden", "pet friendly", "front desk"
    ));


    // ─────────────────────────────────────────────────────────────
    //  MAIN ENTRY POINT
    // ─────────────────────────────────────────────────────────────
    public static Hotel scrapeHotelByName(String hotelName) {


        try (Playwright playwright = Playwright.create();


             Browser browser = playwright.chromium().launch(
                     new BrowserType.LaunchOptions()
                             .setHeadless(false)
             );


             BrowserContext context = browser.newContext(
                     new Browser.NewContextOptions()
                             .setViewportSize(1400, 900)
                             .setUserAgent(
                                     "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                                     "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                     "Chrome/120.0.0.0 Safari/537.36"
                             )
                             .setLocale("en-US")
                             .setExtraHTTPHeaders(
                                     java.util.Map.ofEntries(
                                             java.util.Map.entry("Accept-Language", "en-US,en;q=0.9"),
                                             java.util.Map.entry("Accept",
                                                     "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"),
                                             java.util.Map.entry("Cache-Control", "max-age=0"),
                                             java.util.Map.entry("Upgrade-Insecure-Requests", "1")
                                     )
                             )
             );


             Page page = context.newPage()) {


            LOGGER.info("Searching Agoda hotel: " + hotelName);


            // ── 1. Open Agoda homepage ──────────────────────────────────
            page.navigate(
                    "https://www.agoda.com",
                    new Page.NavigateOptions()
                            .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                            .setTimeout(60000)
            );
            page.waitForTimeout(5000);
            dismissPopups(page);


            // ── 2. Locate and fill the search box ──────────────────────
            Locator searchBox = page.locator(
                    "input[placeholder*='destination'], input[placeholder*='property']"
            ).first();
            searchBox.waitFor();
            searchBox.click();
            searchBox.type(hotelName, new Locator.TypeOptions().setDelay(100));


            // Allow autocomplete suggestions to load
            page.waitForTimeout(5000);


            // ── 3. Select autocomplete option, then click Search if needed ──
            if (!selectDropdownOption(page, hotelName)) {
                LOGGER.warning("Dropdown selection failed – no options found. Aborting.");
                return null;
            }


            // ── 4. Hard guard: abort if we are still on the homepage ────────
            //      selectDropdownOption() already printed URL/title after the
            //      search-button click, so we only need the final check here.
            String currentUrl = page.url();
            System.out.println("FINAL URL CHECK = " + currentUrl);


            if (!isValidResultPage(currentUrl)) {
                LOGGER.warning(
                    "Navigation did not reach a results or hotel page. " +
                    "Current URL: " + currentUrl + " – Aborting.");
                return null;
            }


            System.out.println("Navigation confirmed – proceeding with extraction.");


            // ── 5. We are now on a results page or hotel page ──────────
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            page.waitForTimeout(3000);
            dismissPopups(page);


            List<Hotel> competitors = new ArrayList<>();


            // If we landed on a search-results page, grab competitors first
            if (isSearchResultsPage(page.url())) {
                competitors = extractCompetitorsFromSearchResults(page, 5);
                System.out.println("COMPETITORS FOUND = " + competitors.size());
                LOGGER.info("Agoda result page competitors collected: " + competitors.size());


                // Then navigate into the first hotel card
                if (!clickFirstHotelCard(page)) {
                    LOGGER.warning("Could not click first hotel card on Agoda search results page.");
                    return null;
                }


                page.waitForLoadState(LoadState.DOMCONTENTLOADED);
                page.waitForTimeout(5000);
                dismissPopups(page);


                System.out.println("HOTEL PAGE URL   = " + page.url());
                System.out.println("HOTEL PAGE TITLE = " + page.title());
            }


            // ── 6. Extract hotel details ────────────────────────────────
            Hotel hotel = extractHotel(page, hotelName);
            if (hotel == null) {
                LOGGER.warning("Agoda hotel extraction failed.");
                return null;
            }


            hotel.setCompetitors(competitors);


            if (!competitors.isEmpty()) {
                CompetitorAnalysis analysis = CompetitorAnalyzer.analyze(hotel, competitors);
                hotel.setCompetitorInsights(analysis.toDisplayString());
            } else {
                hotel.setCompetitorInsights(null);
            }


            LOGGER.info("Total Agoda competitors collected: " + competitors.size());
            LOGGER.info("Agoda hotel extracted successfully: " + hotel.getName());
            return hotel;


        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Agoda scraping failed", e);
            return null;
        }
    }


    // ─────────────────────────────────────────────────────────────
    //  DROPDOWN SELECTION  (AriaRole.OPTION with triple fallback)
    // ─────────────────────────────────────────────────────────────


    /**
     * Waits for autocomplete options to appear, prints them all, then:
     *  1. Clicks the first option (three escalating strategies).
     *  2. Waits 2 s.
     *  3. If the URL is still the homepage, clicks the Agoda Search button.
     *  4. Waits for load and prints the final URL / title.
     *
     * Returns false only when no options were found at all.
     * The caller must check isValidResultPage() / isStillOnHomepage() itself.
     */
    private static boolean selectDropdownOption(Page page, String hotelName) {


        // ── Poll for options (AriaRole.OPTION is what Playwright Inspector found) ──
        Locator options = page.getByRole(AriaRole.OPTION);
        int pollAttempts = 0;
        while (options.count() == 0 && pollAttempts < 6) {
            page.waitForTimeout(1000);
            pollAttempts++;
            options = page.getByRole(AriaRole.OPTION);
        }


        int optionCount = options.count();
        System.out.println("OPTIONS FOUND = " + optionCount);


        if (optionCount == 0) {
            System.out.println("ERROR: NO OPTIONS FOUND after polling");
            return false;
        }


        // Print every option for debugging
        for (int i = 0; i < optionCount; i++) {
            try {
                System.out.println("OPTION " + i + " = " + options.nth(i).innerText());
            } catch (Exception ignored) {
                System.out.println("OPTION " + i + " = <could not read text>");
            }
        }


        Locator firstOption = options.first();
        try { firstOption.scrollIntoViewIfNeeded(); } catch (Exception ignored) { }


        // ── Strategy 1: standard forced click ───────────────────────
        System.out.println("STRATEGY 1: standard click on first option");
        try {
            firstOption.click(new Locator.ClickOptions().setForce(true));
            page.waitForTimeout(2000);
            if (!isStillOnHomepage(page.url())) {
                System.out.println("Strategy 1 succeeded (navigated directly).");
                return true;
            }
        } catch (Exception e) {
            System.out.println("Strategy 1 exception: " + e.getMessage());
        }


        // ── Strategy 2: double-click ─────────────────────────────────
        System.out.println("STRATEGY 2: double-click on first option");
        try {
            firstOption.dblclick();
            page.waitForTimeout(2000);
            if (!isStillOnHomepage(page.url())) {
                System.out.println("Strategy 2 succeeded (navigated directly).");
                return true;
            }
        } catch (Exception e) {
            System.out.println("Strategy 2 exception: " + e.getMessage());
        }


        // ── Strategy 3: dispatchEvent("click") ──────────────────────
        System.out.println("STRATEGY 3: dispatchEvent click on first option");
        try {
            firstOption.dispatchEvent("click");
            page.waitForTimeout(2000);
            if (!isStillOnHomepage(page.url())) {
                System.out.println("Strategy 3 succeeded (navigated directly).");
                return true;
            }
        } catch (Exception e) {
            System.out.println("Strategy 3 exception: " + e.getMessage());
        }


        // ── Agoda behaviour: option click populates the search box but does NOT
        //    navigate.  The search button must be clicked explicitly. ────────────
        System.out.println(
            "Option click did not trigger navigation – clicking the Search button.");
        return clickSearchButton(page);
    }


    /**
     * Finds the Agoda search / submit button using four selectors in priority order,
     * clicks the first one that is visible, then waits for the page to load.
     *
     * Prints URL and title after the click so the caller can decide whether to
     * continue scraping.
     *
     * Returns true if a button was found and clicked (regardless of where the
     * page navigated to – the caller must still validate the URL).
     */
    private static boolean clickSearchButton(Page page) {


        String[] searchButtonSelectors = {
                "button[data-selenium='searchButton']",
                "button:has-text('SEARCH')",
                "button[type='submit']",
                "button[aria-label*='Search']"
        };


        for (String selector : searchButtonSelectors) {
            try {
                Locator btn = page.locator(selector);
                if (btn.count() == 0) {
                    System.out.println("Search button not found with: " + selector);
                    continue;
                }
                // Use the first visible instance
                Locator visible = btn.first();
                if (!visible.isVisible()) {
                    System.out.println("Search button found but not visible: " + selector);
                    continue;
                }
                System.out.println("Clicking search button with selector: " + selector);
                visible.click();


                // Wait for the page to start loading and settle
                page.waitForLoadState(LoadState.DOMCONTENTLOADED);
                page.waitForTimeout(5000);


                System.out.println("URL AFTER SEARCH BUTTON   = " + page.url());
                System.out.println("TITLE AFTER SEARCH BUTTON = " + page.title());
                return true;


            } catch (Exception e) {
                System.out.println("Search button click failed for [" + selector + "]: "
                        + e.getMessage());
            }
        }


        System.out.println("ERROR: Could not find or click any search button.");
        return false;
    }


    // ─────────────────────────────────────────────────────────────
    //  NAVIGATION HELPERS
    // ─────────────────────────────────────────────────────────────


    /**
     * Returns true when the URL is still the Agoda homepage (no useful navigation
     * has occurred yet).  Covers all known homepage patterns:
     *   https://www.agoda.com
     *   https://www.agoda.com/
     *   https://www.agoda.com/?ds=...      ← post-autocomplete-click pattern
     *   https://www.agoda.com/?...
     */
    private static boolean isStillOnHomepage(String url) {
        if (url == null) return true;
        String lower = url.toLowerCase();
        // Must be on agoda.com domain
        if (!lower.contains("agoda.com")) return false;
        // Strip scheme + domain to get the path+query portion
        String pathAndQuery = lower
                .replaceFirst("https?://(www\\.)?agoda\\.com", "");
        // Homepage = empty path, "/" only, or "/?..." (query but no real path)
        return pathAndQuery.isEmpty()
                || pathAndQuery.equals("/")
                || pathAndQuery.startsWith("/?");
    }


    /**
     * Returns true when the URL is a real results or hotel-detail page that
     * is safe to scrape.
     */
    private static boolean isValidResultPage(String url) {
        if (url == null) return false;
        return url.contains("/search")
                || url.contains("textToSearch=")
                || url.contains("/city/")
                || url.contains("/country/")
                || url.contains("/hotel/")
                || url.contains("/property/");
    }


    /** Returns true if the URL looks like a search-results page (not a hotel detail page). */
    private static boolean isSearchResultsPage(String url) {
        if (url == null) return false;
        return url.contains("/search")
                || url.contains("textToSearch=")
                || url.contains("/city/")
                || url.contains("/country/");
    }


    // ─────────────────────────────────────────────────────────────
    //  HOTEL EXTRACTION
    // ─────────────────────────────────────────────────────────────


    private static Hotel extractHotel(Page page, String hotelName) {


        Hotel hotel = new Hotel();
        hotel.setSearchQuery(hotelName);
        hotel.setSourceUrl(page.url());


        String text = page.locator("body").innerText();


        // 1. NAME
        String name = hotelName;
        if (name == null || name.isBlank()) {
            name = extractText(page, "h1");
            if (name == null) {
                name = extractText(page, "[data-selenium='hotel-header-name']");
            }
        }
        hotel.setName(name != null ? name : "N/A");


        // 2. PRICE  (DOM-first, currency-symbol-agnostic-separator regex fallback)
        String price = extractAgodaPrice(page, text);
        hotel.setPrice(price);


        // 3. RATING  (JSON-LD first, then strict "digit.digit" pattern only —
        //             never a bare integer like "74" with the decimal point lost)
        String rating = extractAgodaRating(page, text);
        hotel.setRating(rating);


        // 4. REVIEW COUNT
        String reviews = firstNonBlank(
                extractRegex(text, "From\\s*([0-9,]+)\\s+reviews", 1),
                extractRegex(text, "Based on\\s*([0-9,]+)\\s+verified reviews", 1),
                extractRegex(text, "([0-9,]+)\\s+reviews", 1)
        );
        if (reviews != null) reviews = reviews.replace(",", "");
        hotel.setReviews(reviews != null ? reviews : "N/A");


        LOGGER.info("Agoda extract text snippet: " +
                (text.length() > 300 ? text.substring(0, 300) + "..." : text));
        LOGGER.info("Extracted Agoda price = " + hotel.getPrice());
        LOGGER.info("Extracted Agoda rating = " + hotel.getRating());
        LOGGER.info("Extracted Agoda review count = " + hotel.getReviews());


        // 5. ADDRESS
        hotel.setAddress(extractAgodaAddress(page, text));


        // 6. DESCRIPTION
        String description = "N/A";
        int moreAboutIndex = text.indexOf("More about");
        int amenitiesIndex = text.indexOf("Amenities and facilities");
        if (moreAboutIndex >= 0 && amenitiesIndex > moreAboutIndex) {
            String descriptionSection = text.substring(
                    moreAboutIndex + "More about".length(), amenitiesIndex).trim();
            description = normalizeText(descriptionSection);
            if (description == null || description.isEmpty()
                    || description.matches(
                            "(?i).*\\b(show more|see more|top things to do|property policies|" +
                            "amenities and facilities|more about|reviews|book now)\\b.*")) {
                description = "N/A";
            }
        }
        hotel.setDescription(description);


        // 7. AMENITIES (raw extraction first, minimal junk removal only — no whitelist)
        List<String> amenities = extractAgodaAmenities(page, text);
        hotel.setAmenities(amenities);


        // 8. POLICIES
        List<String> policies = new ArrayList<>();
        int policiesIndex = text.indexOf("Property policies");
        int reviewsHeaderIndex = text.indexOf("Reviews");
        if (policiesIndex >= 0 && reviewsHeaderIndex > policiesIndex) {
            String policiesSection = text.substring(
                    policiesIndex + "Property policies".length(), reviewsHeaderIndex);
            for (String line : policiesSection.split("\n")) {
                String cleaned = normalizeText(line);
                if (cleaned == null || cleaned.isEmpty() || cleaned.length() >= 150) continue;
                String lower = cleaned.toLowerCase();

                // Reject quality / rating items and unrelated policy content.
                if (lower.matches("^.*\\b\\d+\\.\\d+$")) continue;
                if (lower.contains("cleanliness") || lower.contains("comfort") || lower.contains("value for money")
                        || lower.contains("location score") || lower.contains("score") || lower.contains("rating")
                        || lower.contains("hotel name") || lower.contains("price") || lower.contains("discount")
                        || lower.contains("coupon") || lower.contains("availability") || lower.contains("left")
                        || lower.contains("important") || lower.contains("house rules") || lower.contains("erode")) continue;

                // Discard items that are clearly amenities, not policies.
                if (lower.contains("wifi") || lower.contains("parking") || lower.contains("family rooms")
                        || lower.contains("room service") || lower.contains("airport shuttle")
                        || lower.contains("spa") || lower.contains("gym") || lower.contains("pool")
                        || lower.contains("restaurant") || lower.contains("breakfast") || lower.contains("bar")
                        || lower.contains("air conditioning") || lower.contains("laundry") || lower.contains("elevator")
                        || lower.contains("terrace") || lower.contains("garden") || lower.contains("24-hour front desk")
                        || lower.contains("non-smoking") || lower.contains("non smoking")) continue;

                if (lower.contains("show more") || lower.contains("see more")
                        || lower.contains("view more") || lower.contains("breadcrumb")
                        || lower.contains("top things to do") || lower.contains("search")
                        || lower.contains("reviews") || lower.contains("book now")
                        || lower.contains("read more")) continue;

                // Only keep valid policy stamps
                if (lower.contains("check-in") || lower.contains("check out") || lower.contains("check-out")
                        || lower.contains("cancellation") || lower.contains("children")
                        || lower.contains("pets") || lower.contains("smoking")
                        || lower.contains("payment") || lower.contains("refund")
                        || lower.contains("damage deposit")) {
                    policies.add(cleaned);
                }
            }
        }
        hotel.setHouseRules(policies);

        // 9. ROOM FEATURES
        List<String> roomFeatures = extractAgodaRoomFeatures(page, text);
        hotel.setRoomFeatures(roomFeatures);


        // 10. ROOMS
        List<RoomOption> rooms = new ArrayList<>();
        int selectRoomIndex = text.indexOf("Select your room");
        int thingsToDoIndex = text.indexOf("Top things to do");
        if (selectRoomIndex >= 0 && thingsToDoIndex > selectRoomIndex) {
            String roomsSection = text.substring(
                    selectRoomIndex + "Select your room".length(), thingsToDoIndex);
            for (String roomLine : roomsSection.split("\n")) {
                String cleaned = normalizeText(roomLine);
                if (cleaned != null && !cleaned.isEmpty() && cleaned.length() > 5) {
                    RoomOption option = new RoomOption();
                    option.setRoomType(cleaned);
                    option.setPrice("N/A");
                    option.setGuests("2 Guests");
                    option.setAvailability("Available");
                    rooms.add(option);
                }
            }
        }
        hotel.setRoomOptions(rooms);


        LOGGER.info("Price = " + hotel.getPrice());
        LOGGER.info("Rating = " + hotel.getRating());
        LOGGER.info("Reviews = " + hotel.getReviews());
        LOGGER.info("Amenities = " + hotel.getAmenities().size());
        LOGGER.info("Policies = " + hotel.getHouseRules().size());
        LOGGER.info("Rooms = " + hotel.getRoomOptions().size());


        // 10. IMAGES
        List<String> images = extractImages(page);
        if (!images.isEmpty()) hotel.setImageUrl(images.get(0));
        ImageAnalyzer.analyzeMultiple(images, hotel);


        // ── FINAL VALUES BEFORE RETURN ──────────────────────────────
        LOGGER.info("FINAL HOTEL PRICE = " + hotel.getPrice());
        LOGGER.info("FINAL HOTEL RATING = " + hotel.getRating());
        LOGGER.info("FINAL HOTEL REVIEWS = " + hotel.getReviews());
        LOGGER.info("FINAL HOTEL AMENITIES = " + hotel.getAmenities());


        return hotel;
    }


    // ─────────────────────────────────────────────────────────────
    //  PRICE EXTRACTION  (fixed: no longer truncates "5,341" -> "5")
    // ─────────────────────────────────────────────────────────────


    /**
     * Extracts the full displayed Agoda price (e.g. "₹5,341", "Rs. 5,341",
     * "INR 5,341"). DOM price nodes are tried first since they are the most
     * reliable source; the full-page-text regex fallback now tolerates the
     * various thousands-separator characters Agoda may render (plain comma,
     * regular space, non-breaking space, narrow no-break space) instead of
     * matching only a literal ASCII comma — which previously caused prices
     * using a different separator glyph to be truncated to their first
     * digit (e.g. "₹5,341" → "₹5").
     */
    private static String extractAgodaPrice(Page page, String text) {

        String domPrice = firstNonBlank(
                extractText(page, "[data-selenium='display-price']"),
                extractText(page, "[data-element-name='final-price']"),
                extractText(page, "[data-selenium='hotel-price']"),
                extractText(page, "strong[data-selenium='price']"),
                extractText(page, "[class*='PropertyCardPrice']"),
                extractText(page, "[class*='FinalPrice']")
        );

        String amount = sanitizePriceAmount(domPrice);

        if (amount == null && text != null && !text.isBlank()) {
            String raw = firstNonBlank(
                    extractRegex(text,
                            "(?:₹|INR|Rs\\.?)\\s*([0-9][0-9,.\\s\\u00A0\\u202F]*[0-9]|[0-9])", 1),
                    extractRegex(text,
                            "([0-9][0-9,.\\s\\u00A0\\u202F]*[0-9]|[0-9])\\s*(?:₹|INR|Rs\\.?)", 1)
            );
            amount = sanitizePriceAmount(raw);
        }

        return amount != null ? "₹ " + amount : "N/A";
    }

    /**
     * Strips currency symbols, thousands separators (comma, space, NBSP,
     * narrow NBSP) and any other non-digit noise from a raw price string,
     * keeping only the digits that make up the actual amount. This is what
     * prevents a separator character that isn't a plain ASCII comma from
     * causing the captured value to be cut short.
     */
    private static String sanitizePriceAmount(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String digitsOnly = raw.replaceAll("[^0-9]", "");
        return digitsOnly.isEmpty() ? null : digitsOnly;
    }


    // ─────────────────────────────────────────────────────────────
    //  RATING EXTRACTION  (fixed: never drops the decimal point)
    // ─────────────────────────────────────────────────────────────


    /**
     * Extracts the Agoda review-score rating, always returning the one-decimal
     * value Agoda displays (e.g. "7.4"). The previous implementation used
     * "[0-9.]+", which matches a plain run of digits even with NO dot present
     * — so when a DOM rating widget rendered its decimal point as a visual
     * separator rather than literal text (yielding raw text like "74"), that
     * bare "74" was accepted as a valid match and downstream formatting
     * turned it into "74.0". This version requires an explicit "digit.digit"
     * pattern, with JSON-LD aggregateRating checked first as the most
     * reliable source.
     */
    private static String extractAgodaRating(Page page, String text) {

        String jsonLdRating = extractRatingFromJsonLd(page);
        if (jsonLdRating != null) return jsonLdRating;

        String domCandidate = firstNonBlank(
                extractText(page, "[data-component='x-sell-activity-rating']"),
                extractText(page, "[data-selenium='hotel-header-review-rating']"),
                extractText(page, "[data-testid='review-score-compact']")
        );
        if (domCandidate != null) {
            String strict = extractRegex(domCandidate, "([0-9]\\.[0-9])", 1);
            if (strict != null) return strict;
        }

        if (text != null && !text.isBlank()) {
            String strict = firstNonBlank(
                    extractRegex(text, "Property's review score\\s*([0-9]\\.[0-9])", 1),
                    extractRegex(text, "review score\\s*([0-9]\\.[0-9])", 1),
                    extractRegex(text, "([0-9]\\.[0-9])\\s*out of\\s*10", 1)
            );
            if (strict != null) return strict;
        }

        return "N/A";
    }

    /** Pulls "aggregateRating.ratingValue" out of any JSON-LD block on the page, if present. */
    private static String extractRatingFromJsonLd(Page page) {
        try {
            for (String json : page.locator("script[type='application/ld+json']").allTextContents()) {
                if (json == null || json.isBlank()) continue;
                String ratingValue = extractRegex(
                        json, "\"ratingValue\"\\s*:\\s*\"?([0-9]+(?:\\.[0-9]+)?)\"?", 1);
                if (ratingValue != null && !ratingValue.isBlank()) {
                    try {
                        double value = Double.parseDouble(ratingValue);
                        return String.format(Locale.US, "%.1f", value);
                    } catch (NumberFormatException ignored) {
                        return ratingValue;
                    }
                }
            }
        } catch (Exception ignored) { }
        return null;
    }


    // ─────────────────────────────────────────────────────────────
    //  AMENITY EXTRACTION  (raw-first, minimal junk removal — no whitelist)
    // ─────────────────────────────────────────────────────────────


    /**
     * Extracts amenities for an Agoda hotel page using strict whitelist filtering.
     *
     * Process:
     *  1. Extract raw text only from DOM-targeted facility sections.
     *  2. Filter out items containing forbidden keywords (cities, discounts, ratings, etc.).
     *  3. Normalize amenity names (WiFi, Gym, Pool, etc.).
     *  4. Keep only whitelisted amenities.
     *  5. Remove duplicates and limit to 20 items.
     */
    private static List<String> extractAgodaAmenities(Page page, String fullText) {
        Set<String> rawAmenities = new LinkedHashSet<>();

        // Extract from DOM facility sections ONLY (not page.textContent())
        rawAmenities.addAll(extractAmenitiesFromDomSections(page));

        // Extract from legacy text parsing (as secondary source)
        if (fullText != null && !fullText.isBlank()) {
            rawAmenities.addAll(extractAmenitiesFromTextSections(fullText));
        }

        LOGGER.info("RAW AGODA AMENITIES COUNT = " + rawAmenities.size());
        LOGGER.info("RAW AGODA AMENITIES = " + rawAmenities);

        // Filter and normalize
        Set<String> normalized = new LinkedHashSet<>();
        for (String raw : rawAmenities) {
            if (raw == null || raw.isBlank()) continue;
            String lower = raw.toLowerCase();

            // Skip if contains forbidden keywords
            boolean forbidden = false;
            for (String keyword : AMENITY_FORBIDDEN_KEYWORDS) {
                if (lower.contains(keyword.toLowerCase())) {
                    forbidden = true;
                    break;
                }
            }
            if (forbidden) continue;

            // Normalize the amenity name
            String normName = normalizeAmenityName(raw);
            if (normName != null && !normName.isEmpty()) {
                normalized.add(normName);
            }
        }

        // Keep only whitelisted amenities
        List<String> result = new ArrayList<>();
        for (String amenity : normalized) {
            if (AMENITY_WHITELIST.contains(amenity) && result.size() < 20) {
                result.add(amenity);
            }
        }

        LOGGER.info("Agoda amenities after whitelist filtering: " + result.size()
                + " (removed " + (rawAmenities.size() - result.size()) + " invalid/duplicate items)");

        return result;
    }

    /** Extract amenities from DOM facility sections only. */
    private static Set<String> extractAmenitiesFromDomSections(Page page) {
        Set<String> amenities = new LinkedHashSet<>();

        try {
            // Find all elements containing facility/amenity section headers
            String[] sectionMarkers = {
                    "xpath=//*[contains(normalize-space(string(.)), 'Most popular facilities')]/following-sibling::*[1]",
                    "xpath=//*[contains(normalize-space(string(.)), 'Facilities')]/following-sibling::*[1]",
                    "xpath=//*[contains(normalize-space(string(.)), 'Property facilities')]/following-sibling::*[1]",
                    "xpath=//*[contains(normalize-space(string(.)), 'Room facilities')]/following-sibling::*[1]",
                    "xpath=//*[contains(normalize-space(string(.)), 'Amenities')]/following-sibling::*[1]"
            };

            for (String xpath : sectionMarkers) {
                try {
                    Locator section = page.locator(xpath);
                    if (section.count() > 0) {
                        String sectionText = section.first().innerText();
                        if (sectionText != null && !sectionText.isBlank()) {
                            for (String line : sectionText.split("\n")) {
                                String cleaned = normalizeText(line);
                                if (cleaned != null && !cleaned.isEmpty() && cleaned.length() < 100) {
                                    amenities.add(cleaned);
                                }
                            }
                        }
                    }
                } catch (Exception ignored) { }
            }

            // Also try direct facility list item selectors
            Locator facilityItems = page.locator(
                    "[class*='Facility'], [class*='facility'], [data-selenium*='facility'], " +
                    "[class*='Amenity'], [class*='amenity'], li[class*='item']"
            );
            for (int i = 0; i < Math.min(facilityItems.count(), 50); i++) {
                try {
                    String itemText = facilityItems.nth(i).innerText();
                    if (itemText != null && !itemText.isBlank() && itemText.length() < 100) {
                        String cleaned = normalizeText(itemText);
                        if (cleaned != null && !cleaned.isEmpty()) {
                            amenities.add(cleaned);
                        }
                    }
                } catch (Exception ignored) { }
            }
        } catch (Exception ignored) { }

        return amenities;
    }

    /** Extract amenities from text sections (legacy/fallback). */
    private static Set<String> extractAmenitiesFromTextSections(String fullText) {
        Set<String> amenities = new LinkedHashSet<>();

        for (String header : AMENITY_SECTION_HEADERS) {
            int start = fullText.indexOf(header);
            if (start < 0) continue;

            int contentStart = start + header.length();
            int end = fullText.length();

            // Find end marker
            for (String endMarker : AMENITY_SECTION_END_MARKERS) {
                int idx = fullText.indexOf(endMarker, contentStart);
                if (idx > contentStart && idx < end) end = idx;
            }

            // Don't let sections swallow each other
            for (String otherHeader : AMENITY_SECTION_HEADERS) {
                if (otherHeader.equals(header)) continue;
                int idx = fullText.indexOf(otherHeader, contentStart);
                if (idx > contentStart && idx < end) end = idx;
            }

            if (end <= contentStart) continue;

            String section = fullText.substring(contentStart, end);
            for (String line : section.split("\n")) {
                String cleaned = normalizeText(line);
                if (cleaned != null && !cleaned.isEmpty() && cleaned.length() < 100) {
                    amenities.add(cleaned);
                }
            }
        }

        return amenities;
    }

    private static List<String> extractAgodaPolicies(String fullText) {
        List<String> policies = new ArrayList<>();
        if (fullText == null || fullText.isBlank()) {
            return policies;
        }

        int policiesIndex = fullText.indexOf("Property policies");
        if (policiesIndex < 0) {
            return policies;
        }

        int contentStart = policiesIndex + "Property policies".length();
        int end = fullText.length();
        for (String endMarker : ROOM_FEATURE_SECTION_END_MARKERS) {
            int idx = fullText.indexOf(endMarker, contentStart);
            if (idx > contentStart && idx < end) {
                end = idx;
            }
        }

        if (end <= contentStart) {
            return policies;
        }

        String section = fullText.substring(contentStart, end);
        for (String line : section.split("\n")) {
            String cleaned = normalizeText(line);
            if (cleaned == null || cleaned.isEmpty() || cleaned.length() >= 150) continue;
            String lower = cleaned.toLowerCase(Locale.ROOT);

            if (isJunkPolicyLine(lower) || isAmenityLabel(lower) || isRoomFeatureLabel(lower)) {
                continue;
            }

            for (String keyword : POLICY_ALLOW_KEYWORDS) {
                if (lower.contains(keyword)) {
                    policies.add(cleaned);
                    break;
                }
            }
        }

        return policies;
    }

    private static List<String> extractAgodaRoomFeatures(Page page, String fullText) {
        Set<String> rawRoomFeatures = new LinkedHashSet<>();

        // Room-specific facility sections first
        rawRoomFeatures.addAll(extractRoomFeaturesFromDomSections(page));

        // Text fallback if the DOM did not capture enough room features
        if (rawRoomFeatures.size() < 5 && fullText != null && !fullText.isBlank()) {
            rawRoomFeatures.addAll(extractRoomFeaturesFromTextSections(fullText));
        }

        List<String> result = new ArrayList<>();
        for (String raw : rawRoomFeatures) {
            if (raw == null || raw.isBlank()) continue;
            String cleaned = normalizeText(raw);
            if (cleaned == null || cleaned.isEmpty() || cleaned.length() >= 120) continue;
            String lower = cleaned.toLowerCase(Locale.ROOT);

            if (isAmenityLabel(lower) || isJunkPolicyLine(lower) || isAreaJunk(lower)) continue;

            String normalizedFeature = normalizeRoomFeatureName(cleaned);
            if (normalizedFeature != null && !normalizedFeature.isEmpty() && !result.contains(normalizedFeature)) {
                result.add(normalizedFeature);
            }
        }

        return result;
    }

    private static Set<String> extractRoomFeaturesFromDomSections(Page page) {
        Set<String> features = new LinkedHashSet<>();
        try {
            for (String header : ROOM_FEATURE_SECTION_HEADERS) {
                String xpath = "xpath=//*[contains(normalize-space(string(.)), '" + header + "')]/following-sibling::*[1]";
                Locator section = page.locator(xpath);
                if (section.count() == 0) continue;
                String sectionText = section.first().innerText();
                if (sectionText == null || sectionText.isBlank()) continue;
                for (String line : sectionText.split("\n")) {
                    String cleaned = normalizeText(line);
                    if (cleaned != null && !cleaned.isEmpty() && cleaned.length() < 100) {
                        features.add(cleaned);
                    }
                }
            }
        } catch (Exception ignored) { }
        return features;
    }

    private static Set<String> extractRoomFeaturesFromTextSections(String fullText) {
        Set<String> features = new LinkedHashSet<>();
        for (String header : ROOM_FEATURE_SECTION_HEADERS) {
            int start = fullText.indexOf(header);
            if (start < 0) continue;

            int contentStart = start + header.length();
            int end = fullText.length();
            for (String endMarker : ROOM_FEATURE_SECTION_END_MARKERS) {
                int idx = fullText.indexOf(endMarker, contentStart);
                if (idx > contentStart && idx < end) end = idx;
            }

            if (end <= contentStart) continue;
            String section = fullText.substring(contentStart, end);
            for (String line : section.split("\n")) {
                String cleaned = normalizeText(line);
                if (cleaned != null && !cleaned.isEmpty() && cleaned.length() < 100) {
                    features.add(cleaned);
                }
            }
        }
        return features;
    }

    private static boolean isJunkPolicyLine(String lower) {
        if (lower.matches("^.*\\b\\d+\\.\\d+$")) return true;
        if (lower.contains("cleanliness") || lower.contains("comfort") || lower.contains("value for money")
                || lower.contains("location score") || lower.contains("score") || lower.contains("rating")
                || lower.contains("hotel name") || lower.contains("price") || lower.contains("discount")
                || lower.contains("coupon") || lower.contains("availability") || lower.contains("left")
                || lower.contains("important") || lower.contains("house rules") || lower.contains("erode")
                || lower.contains("show more") || lower.contains("see more") || lower.contains("view more")
                || lower.contains("breadcrumb") || lower.contains("top things to do") || lower.contains("search")
                || lower.contains("reviews") || lower.contains("book now") || lower.contains("read more")) {
            return true;
        }
        return false;
    }

    private static boolean isAmenityLabel(String lower) {
        if (normalizeAmenityName(lower) != null) {
            return true;
        }
        for (String term : AMENITY_WHITELIST) {
            if (lower.contains(term.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRoomFeatureLabel(String lower) {
        for (String keyword : ROOM_FEATURE_SECTION_HEADERS) {
            if (lower.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAreaJunk(String lower) {
        return lower.contains("show more") || lower.contains("see more") || lower.contains("book now")
                || lower.contains("breadcrumb") || lower.contains("top things to do") || lower.contains("reviews")
                || lower.contains("search") || lower.contains("home") || lower.contains("about this property");
    }

    private static String normalizeRoomFeatureName(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.contains("sea view")) return "Sea View";
        if (lower.contains("city view")) return "City View";
        if (lower.contains("garden view")) return "Garden View";
        if (lower.contains("mountain view")) return "Mountain View";
        if (lower.contains("balcony")) return "Balcony";
        if (lower.contains("terrace")) return "Terrace";
        if (lower.contains("bathtub") || lower.contains("bath")) return "Bathtub";
        if (lower.contains("shower")) return "Shower";
        if (lower.contains("minibar") || lower.contains("mini bar")) return "Minibar";
        if (lower.contains("coffee maker") || lower.contains("coffee machine") || lower.contains("kettle")) return "Coffee Maker";
        if (lower.contains("kitchenette") || lower.contains("kitchen")) return "Kitchenette";
        if (lower.contains("desk") || lower.contains("work space") || lower.contains("work area")) return "Desk";
        if (lower.contains("sofa") || lower.contains("sofa bed")) return "Sofa";
        if (lower.contains("safe")) return "Safe";
        if (lower.contains("walk-in closet") || lower.contains("wardrobe") || lower.contains("closet")) return "Wardrobe";
        if (lower.contains("soundproof")) return "Soundproofing";
        if (lower.contains("blackout")) return "Blackout Curtains";
        if (lower.contains("accessible") || lower.contains("wheelchair")) return "Accessible Room";
        return raw.trim();
    }

    /** Normalize a raw amenity name to standard format. */
    private static String normalizeAmenityName(String raw) {
        if (raw == null || raw.isBlank()) return null;

        String lower = raw.toLowerCase();
        String normalized = raw.trim();

        // Normalize common variants
        if (lower.contains("wifi") || lower.contains("wireless internet")) return "WiFi";
        if (lower.contains("parking")) return lower.contains("free") ? "Free Parking" : "Parking";
        if (lower.contains("restaurant")) return "Restaurant";
        if (lower.contains("pool") || lower.contains("swimming")) return "Pool";
        if (lower.contains("gym") || lower.contains("fitness")) return "Gym";
        if (lower.contains("spa")) return "Spa";
        if (lower.contains("breakfast")) return "Breakfast";
        if (lower.contains("bar")) return "Bar";
        if (lower.contains("room service")) return "Room Service";
        if (lower.contains("airport shuttle") || lower.contains("shuttle")) return "Airport Shuttle";
        if (lower.contains("family room")) return "Family Rooms";
        if (lower.contains("air conditioning") || lower.contains("ac")) return "Air Conditioning";
        if (lower.contains("laundry")) return "Laundry";
        if (lower.contains("elevator") || lower.contains("lift")) return "Elevator";
        if (lower.contains("terrace")) return "Terrace";
        if (lower.contains("garden")) return "Garden";
        if (lower.contains("pet")) return "Pet Friendly";
        if (lower.contains("24-hour") || lower.contains("front desk")) return "24-hour Front Desk";

        return null;
    }

    // ─────────────────────────────────────────────────────────────
    //  COMPETITOR EXTRACTION FROM SEARCH RESULTS
    // ─────────────────────────────────────────────────────────────


    private static List<Hotel> extractCompetitorsFromSearchResults(
            Page page, int maxCompetitors) {


        List<Hotel> competitors = new ArrayList<>();
        Set<String> seenNames = new LinkedHashSet<>();


        try {
            Locator cards = page.locator(
                    "div[data-selenium='hotel-card'], div[data-element-name='hotel-card'], " +
                    "div[class*='HotelCard'], div[class*='hotel-card'], " +
                    "a[href*='/hotel/'], a[href*='/property/']"
            );


            int candidateCount = Math.min(cards.count(), maxCompetitors + 1);
            if (candidateCount <= 1) return competitors;


            for (int i = 1; i < candidateCount && competitors.size() < maxCompetitors; i++) {
                try {
                    Locator card = cards.nth(i);
                    String cardText = normalizeText(card.innerText());
                    if (cardText == null || cardText.isBlank()) continue;


                    String candidateName = firstNonBlank(
                            extractText(card, "[data-selenium='hotel-name']"),
                            extractText(card, "[data-element-name='hotel-name']"),
                            extractText(card, "h3"),
                            extractText(card, "h2"),
                            extractText(card, "h4"),
                            extractText(card, "span")
                    );


                    if (candidateName == null) {
                        for (String line : cardText.split("\\r?\\n")) {
                            String cleaned = normalizeText(line);
                            if (cleaned == null || cleaned.isBlank()) continue;
                            String normalized = cleaned.toLowerCase();
                            if (normalized.contains("see details") || normalized.contains("from rs")
                                    || normalized.contains("review score") || normalized.contains("reviews")
                                    || normalized.contains("room") || normalized.contains("night")
                                    || normalized.contains("guests") || normalized.contains("price")
                                    || normalized.contains("book now") || normalized.contains("view deal")
                                    || normalized.contains("special offer") || normalized.contains("km")
                                    || normalized.contains("mi") || normalized.contains("location"))
                                continue;
                            if (cleaned.length() >= 3 && cleaned.length() <= 80) {
                                candidateName = cleaned;
                                break;
                            }
                        }
                    }


                    if (candidateName == null || candidateName.isBlank()) continue;


                    String normalizedCandidate = normalizeHotelName(candidateName);
                    if (normalizedCandidate.isBlank() || seenNames.contains(normalizedCandidate)) continue;
                    seenNames.add(normalizedCandidate);


                    String competitorPrice = firstNonBlank(
                            extractRegex(cardText,
                                    "(?:₹|INR|Rs\\.?)\\s*([0-9][0-9,.\\s\\u00A0\\u202F]*[0-9]|[0-9])", 1),
                            extractRegex(cardText,
                                    "from\\s*(?:₹|INR|Rs\\.?)\\s*([0-9][0-9,.\\s\\u00A0\\u202F]*[0-9]|[0-9])", 1)
                    );
                    competitorPrice = sanitizePriceAmount(competitorPrice);
                    if (competitorPrice != null) competitorPrice = "₹ " + competitorPrice;


                    String competitorRating = firstNonBlank(
                            extractRegex(cardText, "review score\\s*([0-9]\\.[0-9])", 1),
                            extractRegex(cardText, "([0-9]\\.[0-9])\\s*out of\\s*10", 1),
                            extractRegex(cardText, "\\b([0-9]\\.[0-9])\\b", 1)
                    );


                    String competitorReviews = firstNonBlank(
                            extractRegex(cardText, "([0-9,]+)\\s+reviews", 1)
                    );
                    if (competitorReviews != null) competitorReviews = competitorReviews.replaceAll(",", "");


                    String distance = firstNonBlank(
                            extractRegex(cardText,
                                    "Distance\\s*[:\\-]?\\s*([0-9]+(?:\\.[0-9]+)?\\s*(?:km|mi|miles))", 1),
                            extractRegex(cardText, "([0-9]+(?:\\.[0-9]+)?\\s*(?:km|mi|miles))", 1)
                    );


                    String locationScore = firstNonBlank(
                            extractRegex(cardText, "Location\\s*score\\s*[:\\-]?\\s*([0-9]+(?:\\.[0-9]+)?)", 1),
                            extractRegex(cardText, "Location\\s*[:\\-]?\\s*([0-9]+(?:\\.[0-9]+)?)", 1)
                    );


                    String sourceUrl = extractAttribute(card, "a", "href");
                    if (sourceUrl != null && sourceUrl.startsWith("/")) sourceUrl = AGODA_BASE_URL + sourceUrl;


                    Hotel competitor = new Hotel();
                    competitor.setName(candidateName);
                    competitor.setPrice(competitorPrice != null ? competitorPrice : "N/A");
                    competitor.setRating(competitorRating != null ? competitorRating : "N/A");
                    competitor.setReviews(competitorReviews != null ? competitorReviews : "N/A");
                    competitor.setDistance(distance != null ? distance : "N/A");
                    competitor.setLocationScore(locationScore != null ? locationScore : "N/A");
                    competitor.setSourceUrl(sourceUrl != null ? sourceUrl : page.url());


                    LOGGER.info("COMPETITOR => " + competitor.getName()
                            + " | PRICE=" + competitor.getPrice()
                            + " | RATING=" + competitor.getRating()
                            + " | REVIEWS=" + competitor.getReviews());


                    boolean onlyHasName = "N/A".equals(competitor.getPrice())
                            && "N/A".equals(competitor.getRating())
                            && "N/A".equals(competitor.getReviews());
                    if (onlyHasName) {
                        LOGGER.info("Skipping competitor with no price/rating/reviews data: "
                                + competitor.getName());
                        continue;
                    }


                    competitors.add(competitor);
                } catch (Exception ignored) { }
            }
        } catch (Exception ignored) { }


        return competitors;
    }


    // ─────────────────────────────────────────────────────────────
    //  COMPETITOR EXTRACTION FROM HOTEL PAGE
    // ─────────────────────────────────────────────────────────────


    private static List<Hotel> extractCompetitorsFromHotelPage(
            Page page, String hotelName, int maxCompetitors) {


        List<Hotel> competitors = new ArrayList<>();
        Set<String> seenNames = new LinkedHashSet<>();


        String[] sectionTitles = {
                "Similar properties", "Hotels nearby", "Recommended properties",
                "People also viewed", "Other travellers viewed", "You may also like",
                "More hotels you might like", "More properties"
        };


        for (String title : sectionTitles) {
            try {
                Locator section = page.locator(
                        "xpath=//*[contains(normalize-space(string(.)), '" + title + "')]"
                ).first();
                if (section == null || section.count() == 0) continue;


                Locator cards = section.locator(
                        "div[data-selenium='hotel-card'], div[data-element-name='hotel-card'], " +
                        "div[class*='HotelCard'], div[class*='hotel-card'], " +
                        "a[href*='/hotel/'], a[href*='/property/']"
                );
                if (cards.count() == 0) continue;


                int candidateCount = Math.min(cards.count(), maxCompetitors * 4);
                for (int i = 0; i < candidateCount && competitors.size() < maxCompetitors; i++) {
                    try {
                        Locator card = cards.nth(i);
                        String candidateName = firstNonBlank(
                                extractText(card, "[data-selenium='hotel-name']"),
                                extractText(card, "[data-element-name='hotel-name']"),
                                extractText(card, "h3"),
                                extractText(card, "h2"),
                                extractText(card, "h4"),
                                extractText(card, "span")
                        );


                        if (candidateName == null || candidateName.isBlank()) continue;
                        if (hotelName != null && !hotelName.isBlank()
                                && isSameHotel(candidateName, hotelName)) continue;


                        String normalizedCandidate = normalizeHotelName(candidateName);
                        if (normalizedCandidate.isBlank() || seenNames.contains(normalizedCandidate)) continue;
                        seenNames.add(normalizedCandidate);


                        String cardText = normalizeText(card.innerText());
                        String competitorPrice = firstNonBlank(
                                extractRegex(cardText, "Current price:\\s*Rs\\.?\\s*([0-9,.\\s\\u00A0\\u202F]+)", 1),
                                extractRegex(cardText, "from\\s+Rs\\.?\\s*([0-9,.\\s\\u00A0\\u202F]+)", 1),
                                extractRegex(cardText, "Rs\\.?\\s*([0-9,.\\s\\u00A0\\u202F]+)", 1)
                        );
                        competitorPrice = sanitizePriceAmount(competitorPrice);
                        if (competitorPrice != null) competitorPrice = "Rs. " + competitorPrice;


                        String competitorRating = firstNonBlank(
                                extractRegex(cardText, "review score\\s*([0-9]\\.[0-9])", 1),
                                extractRegex(cardText, "([0-9]\\.[0-9])\\s*out of\\s*10", 1)
                        );
                        String competitorReviews = firstNonBlank(
                                extractRegex(cardText, "([0-9,]+)\\s+reviews", 1)
                        );
                        if (competitorReviews != null) competitorReviews = competitorReviews.replace(",", "");


                        Hotel competitor = new Hotel();
                        competitor.setName(candidateName);
                        competitor.setPrice(competitorPrice != null ? competitorPrice : "N/A");
                        competitor.setRating(competitorRating != null ? competitorRating : "N/A");
                        competitor.setReviews(competitorReviews != null ? competitorReviews : "N/A");
                        competitor.setSourceUrl(firstNonBlank(
                                extractAttribute(card, "a", "href"), page.url()));


                        LOGGER.info("COMPETITOR => " + competitor.getName()
                                + " | PRICE=" + competitor.getPrice()
                                + " | RATING=" + competitor.getRating()
                                + " | REVIEWS=" + competitor.getReviews());


                        boolean onlyHasName = "N/A".equals(competitor.getPrice())
                                && "N/A".equals(competitor.getRating())
                                && "N/A".equals(competitor.getReviews());
                        if (onlyHasName) {
                            LOGGER.info("Skipping competitor with no price/rating/reviews data: "
                                    + competitor.getName());
                            continue;
                        }


                        competitors.add(competitor);
                    } catch (Exception ignored) { }
                }


                if (!competitors.isEmpty()) {
                    LOGGER.info("Found " + competitors.size()
                            + " competitors in hotel page section: " + title);
                    return competitors;
                }
            } catch (Exception ignored) { }
        }


        return competitors;
    }


    // ─────────────────────────────────────────────────────────────
    //  CITY FALLBACK
    // ─────────────────────────────────────────────────────────────


    private static List<Hotel> extractCompetitorsByCityFallback(
            Page page, Hotel hotel, int maxCompetitors) {


        String extractedCity = extractCityFromPage(page, hotel.getAddress());
        if (extractedCity == null || extractedCity.isBlank()) {
            LOGGER.warning("Could not extract city from Agoda page. Skipping fallback.");
            return List.of();
        }


        LOGGER.info("Extracted city from Agoda page: " + extractedCity);
        String fallbackUrl = buildCitySearchUrl(extractedCity);


        try {
            page.navigate(fallbackUrl,
                    new Page.NavigateOptions()
                            .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                            .setTimeout(60000));
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);
            page.waitForTimeout(8000);
            dismissPopups(page);


            List<Hotel> competitors = extractCompetitorsCitySearch(page, hotel.getName(), maxCompetitors);
            LOGGER.info("City fallback returned " + competitors.size()
                    + " competitors from " + extractedCity);
            return competitors;
        } catch (Exception e) {
            LOGGER.warning("City fallback failed for " + extractedCity + ": " + e.getMessage());
            return List.of();
        }
    }


    private static String extractCityFromPage(Page page, String address) {
        String city = extractCityFromJsonLd(page);
        if (city != null && !city.isBlank()) return city;


        try {
            String breadcrumb = extractText(page,
                    "nav[aria-label='breadcrumb'], [role='navigation'] span, .breadcrumb");
            if (breadcrumb != null && !breadcrumb.isBlank()) {
                String[] parts = breadcrumb.split(">");
                if (parts.length > 0) {
                    String candidate = parts[parts.length - 1].trim();
                    if (candidate.length() > 2 && !candidate.matches(".*\\d.*")) return candidate;
                }
            }
        } catch (Exception ignored) { }


        try {
            String locationText = extractText(page,
                    "[data-testid='location-heading'], [class*='location'], " +
                    "[class*='Location'], [data-selenium='hotel-address']");
            if (locationText != null && !locationText.isBlank()) {
                String candidate = locationText.split(",")[0].trim();
                if (candidate.length() > 2 && !candidate.matches(".*\\d.*")) return candidate;
            }
        } catch (Exception ignored) { }


        if (address != null && !address.isBlank() && !address.equals("N/A")) {
            city = extractCityFromAddress(address);
            if (city != null && !city.isBlank()) return city;
        }


        try {
            String pageText = page.locator("body").innerText();
            String[] lines = pageText.split("\n");
            for (int i = 0; i < lines.length; i++) {
                String cleaned = normalizeText(lines[i]);
                if (cleaned != null && cleaned.toLowerCase().contains("map")) {
                    if (i + 1 < lines.length) {
                        String nextLine = normalizeText(lines[i + 1]);
                        if (nextLine != null && nextLine.length() > 2
                                && !nextLine.matches(".*\\d+.*")) return nextLine;
                    }
                }
            }
        } catch (Exception ignored) { }


        return null;
    }


    private static String extractCityFromAddress(String address) {
        if (address == null || address.isBlank()) return null;
        String[] parts = address.split(",");
        for (int i = parts.length - 1; i >= 0; i--) {
            String part = parts[i].trim();
            if (part.length() > 2 && !part.matches("\\d+.*")) return part;
        }
        return address.trim();
    }


    private static List<Hotel> extractCompetitorsCitySearch(
            Page page, String hotelName, int maxCompetitors) {


        List<Hotel> competitors = new ArrayList<>();
        Set<String> seenNames = new LinkedHashSet<>();


        String[] cardSelectors = {
                "div[data-selenium='hotel-card']",
                "div[data-element-name='hotel-card']",
                "div[class*='HotelCard']",
                "div[class*='hotel-card']",
                "a[href*='/hotel/']",
                "a[href*='/property/']"
        };


        for (String selector : cardSelectors) {
            try {
                Locator cards = page.locator(selector);
                int candidateCount = Math.min(cards.count(), maxCompetitors + 1);
                if (candidateCount <= 1) continue;


                for (int i = 1; i < candidateCount && competitors.size() < maxCompetitors; i++) {
                    try {
                        Locator card = cards.nth(i);
                        String cardText = normalizeText(card.innerText());
                        if (cardText == null || cardText.isBlank()) continue;


                        String candidateName = firstNonBlank(
                                extractText(card, "[data-selenium='hotel-name']"),
                                extractText(card, "[data-element-name='hotel-name']"),
                                extractText(card, "h3"),
                                extractText(card, "h2"),
                                extractText(card, "h4"),
                                extractText(card, "span")
                        );


                        if (candidateName == null || candidateName.isBlank()) {
                            for (String line : cardText.split("\\r?\\n")) {
                                String cleaned = normalizeText(line);
                                if (cleaned == null || cleaned.isBlank()) continue;
                                String normalized = cleaned.toLowerCase();
                                if (normalized.contains("see details") || normalized.contains("from rs")
                                        || normalized.contains("review score") || normalized.contains("reviews")
                                        || normalized.contains("room") || normalized.contains("night")
                                        || normalized.contains("guests") || normalized.contains("price")
                                        || normalized.contains("book now") || normalized.contains("view deal")
                                        || normalized.contains("special offer")) continue;
                                if (cleaned.length() >= 3 && cleaned.length() <= 80) {
                                    candidateName = cleaned;
                                    break;
                                }
                            }
                        }


                        if (candidateName == null || candidateName.isBlank()) continue;
                        if (hotelName != null && !hotelName.isBlank()
                                && isSameHotel(candidateName, hotelName)) continue;


                        String normalizedCandidate = normalizeHotelName(candidateName);
                        if (normalizedCandidate.isBlank() || seenNames.contains(normalizedCandidate)) continue;
                        seenNames.add(normalizedCandidate);


                        String competitorPrice = firstNonBlank(
                                extractRegex(cardText,
                                        "(?:₹|INR|Rs\\.?)\\s*([0-9][0-9,.\\s\\u00A0\\u202F]*[0-9]|[0-9])", 1),
                                extractRegex(cardText,
                                        "from\\s*(?:₹|INR|Rs\\.?)\\s*([0-9][0-9,.\\s\\u00A0\\u202F]*[0-9]|[0-9])", 1)
                        );
                        competitorPrice = sanitizePriceAmount(competitorPrice);
                        if (competitorPrice != null) competitorPrice = "₹ " + competitorPrice;


                        String competitorRating = firstNonBlank(
                                extractRegex(cardText, "review score\\s*([0-9]\\.[0-9])", 1),
                                extractRegex(cardText, "([0-9]\\.[0-9])\\s*out of\\s*10", 1)
                        );
                        String competitorReviews = firstNonBlank(
                                extractRegex(cardText, "([0-9,]+)\\s+reviews", 1)
                        );
                        if (competitorReviews != null) competitorReviews = competitorReviews.replaceAll(",", "");


                        String sourceUrl = extractAttribute(card, "a", "href");
                        if (sourceUrl != null && sourceUrl.startsWith("/")) sourceUrl = AGODA_BASE_URL + sourceUrl;


                        Hotel competitor = new Hotel();
                        competitor.setName(candidateName);
                        competitor.setPrice(competitorPrice != null ? competitorPrice : "N/A");
                        competitor.setRating(competitorRating != null ? competitorRating : "N/A");
                        competitor.setReviews(competitorReviews != null ? competitorReviews : "N/A");
                        competitor.setSourceUrl(sourceUrl != null ? sourceUrl : page.url());


                        LOGGER.info("COMPETITOR => " + competitor.getName()
                                + " | PRICE=" + competitor.getPrice()
                                + " | RATING=" + competitor.getRating()
                                + " | REVIEWS=" + competitor.getReviews());


                        boolean onlyHasName = "N/A".equals(competitor.getPrice())
                                && "N/A".equals(competitor.getRating())
                                && "N/A".equals(competitor.getReviews());
                        if (onlyHasName) {
                            LOGGER.info("Skipping competitor with no price/rating/reviews data: "
                                    + competitor.getName());
                            continue;
                        }


                        competitors.add(competitor);
                    } catch (Exception ignored) { }
                }


                if (!competitors.isEmpty()) {
                    LOGGER.info("Found " + competitors.size()
                            + " city search competitors using selector: " + selector);
                    return competitors;
                }
            } catch (Exception ignored) { }
        }


        return competitors;
    }


    // ─────────────────────────────────────────────────────────────
    //  ADDRESS / IMAGE HELPERS
    // ─────────────────────────────────────────────────────────────


    private static String extractAgodaAddress(Page page, String text) {
        String address = firstNonBlank(
                extractText(page, "[data-selenium='hotel-address']"),
                extractText(page, "[data-testid='hotel-address']"),
                extractText(page, "[data-testid='address']"),
                extractText(page, "div[class*='Address']"),
                extractText(page, "[class*='address']")
        );
        if (address != null && !address.isBlank()) return address;


        address = extractAddressFromJsonLd(page);
        if (address != null && !address.isBlank()) return address;


        String fallback = extractRegex(text, "(?:Address|Location)\\s*[:\\-]?\\s*([^\\n]{10,150})", 1);
        if (fallback != null && fallback.length() > 10) return normalizeText(fallback);


        return "N/A";
    }


    private static String extractAddressFromJsonLd(Page page) {
        try {
            for (String json : page.locator("script[type='application/ld+json']").allTextContents()) {
                if (json == null || json.isBlank()) continue;
                String street   = extractRegex(json, "\"streetAddress\"\\s*:\\s*\"([^\"]+)\"", 1);
                String locality = extractRegex(json, "\"addressLocality\"\\s*:\\s*\"([^\"]+)\"", 1);
                String region   = extractRegex(json, "\"addressRegion\"\\s*:\\s*\"([^\"]+)\"", 1);
                String postal   = extractRegex(json, "\"postalCode\"\\s*:\\s*\"([^\"]+)\"", 1);
                String country  = extractRegex(json, "\"addressCountry\"\\s*:\\s*\"([^\"]+)\"", 1);
                List<String> parts = new ArrayList<>();
                if (street   != null) parts.add(street.trim());
                if (locality != null) parts.add(locality.trim());
                if (region   != null) parts.add(region.trim());
                if (postal   != null) parts.add(postal.trim());
                if (country  != null) parts.add(country.trim());
                if (!parts.isEmpty()) return normalizeText(String.join(", ", parts));
            }
        } catch (Exception ignored) { }
        return null;
    }


    private static String extractCityFromJsonLd(Page page) {
        try {
            for (String json : page.locator("script[type='application/ld+json']").allTextContents()) {
                if (json == null || json.isBlank()) continue;
                String locality = firstNonBlank(
                        extractRegex(json, "\"addressLocality\"\\s*:\\s*\"([^\"]+)\"", 1),
                        extractRegex(json, "\"addressRegion\"\\s*:\\s*\"([^\"]+)\"", 1),
                        extractRegex(json, "\"addressCountry\"\\s*:\\s*\"([^\"]+)\"", 1)
                );
                if (locality != null && locality.length() > 2) return normalizeText(locality);
            }
        } catch (Exception ignored) { }
        return null;
    }


    private static String buildCitySearchUrl(String city) {
        try {
            String encoded = URLEncoder.encode(city, StandardCharsets.UTF_8.toString());
            LocalDate checkIn = LocalDate.now().plusDays(7);
            return AGODA_BASE_URL + "/search?locale=en-us"
                    + "&checkIn=" + checkIn
                    + "&los=1&rooms=1&adults=2&children=0"
                    + "&textToSearch=" + encoded;
        } catch (UnsupportedEncodingException e) {
            return AGODA_BASE_URL + "/search?textToSearch=" + city;
        }
    }


    private static List<String> extractImages(Page page) {
        List<String> images = new ArrayList<>();
        try {
            Locator imgs = page.locator("img");
            for (int i = 0; i < imgs.count(); i++) {
                addImageUrl(images, imgs.nth(i).getAttribute("src"));
                addImageUrl(images, imgs.nth(i).getAttribute("data-src"));
                addImageUrl(images, imgs.nth(i).getAttribute("data-lazy-src"));
                String srcset = imgs.nth(i).getAttribute("srcset");
                if (srcset != null) {
                    for (String part : srcset.split(",")) {
                        addImageUrl(images, part.trim().split(" ")[0]);
                    }
                }
            }
        } catch (Exception ignored) { }
        return images;
    }


    private static void addImageUrl(List<String> images, String url) {
        if (url == null || url.isBlank()) return;
        url = url.trim();
        if (url.startsWith("//")) url = "https:" + url;
        if (!url.startsWith("http")) return;
        if (url.contains("icon") || url.contains("flags") || url.contains("svg")
                || url.contains("data:") || url.contains("banner")) return;
        if (!images.contains(url)) images.add(url);
    }


    // ─────────────────────────────────────────────────────────────
    //  UTILITY HELPERS
    // ─────────────────────────────────────────────────────────────


    private static boolean clickFirstHotelCard(Page page) {
        String[] cardSelectors = {
                "div[data-selenium='hotel-card']",
                "div[data-element-name='hotel-card']",
                "div[class*='HotelCard']",
                "div[class*='hotel-card']",
                "a[href*='/hotel/']",
                "a[href*='/property/']"
        };


        for (String selector : cardSelectors) {
            try {
                Locator cards = page.locator(selector);
                if (cards.count() == 0) continue;


                for (int i = 0; i < cards.count(); i++) {
                    Locator card = cards.nth(i);
                    if (!card.isVisible()) continue;


                    try {
                        Locator link = card.locator(
                                "a[href*='/hotel/'], a[href*='/property/']").first();
                        if (link != null && link.count() > 0 && link.isVisible()) {
                            link.click();
                            return true;
                        }
                    } catch (Exception ignored) { }


                    try {
                        card.click();
                        return true;
                    } catch (Exception ignored) { }
                }
            } catch (Exception ignored) { }
        }
        LOGGER.warning("Failed to click first hotel card on Agoda results page.");
        return false;
    }


    private static boolean isSameHotel(String firstName, String secondName) {
        if (firstName == null || secondName == null) return false;
        String a = normalizeHotelName(firstName);
        String b = normalizeHotelName(secondName);
        if (a.isEmpty() || b.isEmpty()) return false;
        return a.equals(b) || a.contains(b) || b.contains(a);
    }


    private static String normalizeHotelName(String name) {
        if (name == null) return "";
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]+", " ")
                .replaceAll("\\b(hotel|resort|inn|motel|apartment|lodge|bnb|"
                        + "guest house|hostel|stay|villa)\\b", "")
                .replaceAll("\\s+", " ")
                .trim();
    }


    private static void dismissPopups(Page page) {
        clickIfVisible(page, "button[aria-label='Close']");
        clickIfVisible(page, "button:has-text('Accept')");
        clickIfVisible(page, "#onetrust-accept-btn-handler");
    }


    private static void clickIfVisible(Page page, String selector) {
        try {
            Locator locator = page.locator(selector);
            if (locator.count() > 0 && locator.first().isVisible()) locator.first().click();
        } catch (Exception ignored) { }
    }


    private static String extractText(Page page, String selector) {
        try {
            Locator locator = page.locator(selector);
            if (locator.count() == 0) return null;
            return normalizeText(locator.first().innerText());
        } catch (Exception ignored) {
            return null;
        }
    }


    private static String extractText(Locator root, String selector) {
        try {
            Locator locator = root.locator(selector);
            if (locator.count() == 0) return null;
            return normalizeText(locator.first().innerText());
        } catch (Exception ignored) {
            return null;
        }
    }


    private static String extractAttribute(Locator root, String selector, String attribute) {
        try {
            Locator locator = root.locator(selector);
            if (locator.count() == 0) return null;
            return locator.first().getAttribute(attribute);
        } catch (Exception ignored) {
            return null;
        }
    }


    private static String extractRegex(String text, String regex, int group) {
        if (text == null || text.isBlank()) return null;
        java.util.regex.Pattern pattern =
                java.util.regex.Pattern.compile(regex, java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(text);
        return matcher.find() ? normalizeText(matcher.group(group)) : null;
    }


    private static String normalizeText(String text) {
        if (text == null) return null;
        text = text.replaceAll("\\s+", " ").trim();
        return text.isEmpty() ? null : text;
    }


    private static String firstNonBlank(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return null;
    }


    private static String buildSearchUrl(String hotelName) {
        try {
            String encoded = URLEncoder.encode(hotelName, StandardCharsets.UTF_8.toString());
            LocalDate checkIn = LocalDate.now().plusDays(7);
            return AGODA_BASE_URL + "/search?city=0&locale=en-us"
                    + "&checkIn=" + checkIn
                    + "&los=1&rooms=1&adults=2&children=0&textToSearch=" + encoded;
        } catch (UnsupportedEncodingException e) {
            return AGODA_BASE_URL + "/search?textToSearch=" + hotelName;
        }
    }
}