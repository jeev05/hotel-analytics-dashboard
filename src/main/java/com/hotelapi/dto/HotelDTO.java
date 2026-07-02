package com.hotelapi.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Hotel data transfer object for API responses
 */
public class HotelDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String name;
    private String address;
    private String price;
    private String rating;
    private String reviews;
    private String reviewCount;
    private String locationScore;
    private String description;
    private List<String> amenities;
    private List<String> policies;
    private List<String> houseRules;
    private List<String> roomFeatures;
    private String sourceUrl;
    private String imageUrl;
    private double imageScore;
    private boolean blurry;

    public HotelDTO() {
    }

    public HotelDTO(String name, String address, String price, 
                   String rating, String reviews, List<String> amenities) {
        this.name = name;
        this.address = address;
        this.price = price;
        this.rating = rating;
        this.reviews = reviews;
        this.amenities = amenities;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getReviews() {
        return reviews;
    }

    public void setReviews(String reviews) {
        this.reviews = reviews;
    }

    public String getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(String reviewCount) {
        this.reviewCount = reviewCount;
    }

    public String getLocationScore() {
        return locationScore;
    }

    public void setLocationScore(String locationScore) {
        this.locationScore = locationScore;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getAmenities() {
        return amenities;
    }

    public void setAmenities(List<String> amenities) {
        this.amenities = amenities;
    }

    public List<String> getPolicies() {
        return policies;
    }

    public void setPolicies(List<String> policies) {
        this.policies = policies;
    }

    public List<String> getHouseRules() {
        return houseRules;
    }

    public void setHouseRules(List<String> houseRules) {
        this.houseRules = houseRules;
    }

    public List<String> getRoomFeatures() {
        return roomFeatures;
    }

    public void setRoomFeatures(List<String> roomFeatures) {
        this.roomFeatures = roomFeatures;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public double getImageScore() {
        return imageScore;
    }

    public void setImageScore(double imageScore) {
        this.imageScore = imageScore;
    }

    public boolean isBlurry() {
        return blurry;
    }

    public void setBlurry(boolean blurry) {
        this.blurry = blurry;
    }
}
