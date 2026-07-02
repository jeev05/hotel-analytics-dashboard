package model;

import java.util.ArrayList;
import java.util.List;

public class Hotel {

private String searchQuery = "N/A";
private String sourceUrl = "N/A";
private String name = "N/A";
private String address = "N/A";
private String price = "N/A";
private String rating = "N/A";
private String reviews = "N/A";
private String distance = "N/A";
private String locationScore = "N/A";
private String description = "N/A";

// 🔥 IMAGE ANALYSIS FIELDS
private String imageUrl = "N/A";
private double imageScore = 0.0;
private boolean blurry = false;

private final List<String> amenities = new ArrayList<>();
private final List<String> houseRules = new ArrayList<>();
private final List<String> roomFeatures = new ArrayList<>();
private final List<RoomOption> roomOptions = new ArrayList<>();
private final List<Hotel> competitors = new ArrayList<>();
private String competitorInsights = "N/A";

// ✅ GETTERS & SETTERS

public String getSearchQuery() { return "N/A".equals(searchQuery) ? null : searchQuery; }
public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }

public String getSourceUrl() { return "N/A".equals(sourceUrl) ? null : sourceUrl; }
public void setSourceUrl(String sourceUrl) { this.sourceUrl = sourceUrl; }

public String getName() { return "N/A".equals(name) ? null : name; }
public void setName(String name) { this.name = name; }

public String getAddress() { return "N/A".equals(address) ? null : address; }
public void setAddress(String address) { this.address = address; }

public String getPrice() { return "N/A".equals(price) ? null : price; }
public void setPrice(String price) { this.price = price; }

public String getRating() { return "N/A".equals(rating) ? null : rating; }
public void setRating(String rating) { this.rating = rating; }

public String getReviews() { return "N/A".equals(reviews) ? null : reviews; }
public void setReviews(String reviews) { this.reviews = reviews; }

public String getDistance() { return "N/A".equals(distance) ? null : distance; }
public void setDistance(String distance) { this.distance = distance; }

public String getLocationScore() { return "N/A".equals(locationScore) ? null : locationScore; }
public void setLocationScore(String locationScore) { this.locationScore = locationScore; }

public String getDescription() { return "N/A".equals(description) ? null : description; }
public void setDescription(String description) { this.description = description; }

public List<String> getAmenities() { return amenities; }
public void setAmenities(List<String> items) {
    amenities.clear();
    amenities.addAll(items);
}

public List<String> getHouseRules() { return houseRules; }
public void setHouseRules(List<String> items) {
    houseRules.clear();
    houseRules.addAll(items);
}

public List<String> getRoomFeatures() { return roomFeatures; }
public void setRoomFeatures(List<String> items) {
    roomFeatures.clear();
    roomFeatures.addAll(items);
}

public List<RoomOption> getRoomOptions() { return roomOptions; }
public void setRoomOptions(List<RoomOption> items) {
    roomOptions.clear();
    roomOptions.addAll(items);
}

public List<Hotel> getCompetitors() { return competitors; }
public void setCompetitors(List<Hotel> items) {
    competitors.clear();
    competitors.addAll(items);
}

public String getCompetitorInsights() { return "N/A".equals(competitorInsights) ? null : competitorInsights; }
public void setCompetitorInsights(String competitorInsights) {
    this.competitorInsights = competitorInsights;
}

// 🔥 IMAGE GETTERS/SETTERS

public String getImageUrl() { return imageUrl; }
public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

public double getImageScore() { return imageScore; }
public void setImageScore(double imageScore) { this.imageScore = imageScore; }

public boolean isBlurry() { return blurry; }
public void setBlurry(boolean blurry) { this.blurry = blurry; }

// ✅ DISPLAY METHOD

public String toDisplayString() {
    StringBuilder builder = new StringBuilder();
    builder.append("HOTEL DETAILS\n");
    builder.append("-------------\n");
    builder.append("Hotel Name: ").append(getName() == null ? "Data unavailable" : getName()).append('\n');
    builder.append("Price: ").append(getPrice() == null ? "Data unavailable" : getPrice()).append('\n');
    builder.append("Rating: ").append(getRating() == null ? "Data unavailable" : getRating()).append('\n');
    builder.append("Distance: ").append(getDistance() == null ? "Data unavailable" : getDistance()).append('\n');
    builder.append("Location Score: ").append(getLocationScore() == null ? "Data unavailable" : getLocationScore()).append('\n');

    // 🔥 SHOW IMAGE ANALYSIS
    builder.append("Image Score: ").append(imageScore).append('\n');
    builder.append("Blurry: ").append(blurry).append('\n');

    return builder.toString();
}

// ===== EXISTING METHODS (UNCHANGED) =====

}
