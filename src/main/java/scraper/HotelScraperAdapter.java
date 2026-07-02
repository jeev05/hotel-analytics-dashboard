package scraper;

import model.Hotel;
import com.microsoft.playwright.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Public adapter for HotelScraper that exposes key private methods
 * for use by REST API service layer
 */
public class HotelScraperAdapter {
    
    private static final Logger LOGGER = Logger.getLogger(HotelScraperAdapter.class.getName());

    /**
     * Scrapes a hotel using the existing HotelScraper logic
     */
    public static Hotel scrapeHotelByName(String hotelName) {
        try {
            return HotelScraper.scrapeHotelByName(hotelName);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in HotelScraperAdapter: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Test method to validate scraper works
     */
    public static boolean testConnection() {
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch(
                     new BrowserType.LaunchOptions().setHeadless(true))) {
            browser.close();
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Connection test failed", e);
            return false;
        }
    }
}
