package scraper;

import model.Hotel;

public class AgodaScraperTestRunner {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java scraper.AgodaScraperTestRunner <Hotel Name>");
            return;
        }
        String hotelName = String.join(" ", args);
        System.out.println("Scraping Agoda for hotel: " + hotelName);
        Hotel hotel = AgodaScraper.scrapeHotelByName(hotelName);
        if (hotel == null) {
            System.out.println("No hotel data found or scraping failed.");
        } else {
            System.out.println("Hotel scraped successfully:");
            System.out.println(hotel);
        }
    }
}
