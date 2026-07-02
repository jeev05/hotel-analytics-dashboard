package com.hotelapi.dto;

import java.io.Serializable;
import java.util.List;

public class BookingAgodaComparisonDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String bookingPrice;
    private String agodaPrice;

    private String bookingRating;
    private String agodaRating;

    private String bookingReviewCount;
    private String agodaReviewCount;

    private int bookingAmenitiesCount;
    private int agodaAmenitiesCount;

    private List<String> commonAmenities;
    private List<String> bookingOnlyAmenities;
    private List<String> agodaOnlyAmenities;

    private String ratingWinner;
    private String amenitiesWinner;

    private List<String> improvementSuggestions;

    private String overallRecommendation;

    public BookingAgodaComparisonDTO() {
    }

    public String getBookingPrice() {
        return bookingPrice;
    }

    public void setBookingPrice(String bookingPrice) {
        this.bookingPrice = bookingPrice;
    }

    public String getAgodaPrice() {
        return agodaPrice;
    }

    public void setAgodaPrice(String agodaPrice) {
        this.agodaPrice = agodaPrice;
    }

    public String getBookingRating() {
        return bookingRating;
    }

    public void setBookingRating(String bookingRating) {
        this.bookingRating = bookingRating;
    }

    public String getAgodaRating() {
        return agodaRating;
    }

    public void setAgodaRating(String agodaRating) {
        this.agodaRating = agodaRating;
    }

    public String getBookingReviewCount() {
        return bookingReviewCount;
    }

    public void setBookingReviewCount(String bookingReviewCount) {
        this.bookingReviewCount = bookingReviewCount;
    }

    public String getAgodaReviewCount() {
        return agodaReviewCount;
    }

    public void setAgodaReviewCount(String agodaReviewCount) {
        this.agodaReviewCount = agodaReviewCount;
    }

    public int getBookingAmenitiesCount() {
        return bookingAmenitiesCount;
    }

    public void setBookingAmenitiesCount(int bookingAmenitiesCount) {
        this.bookingAmenitiesCount = bookingAmenitiesCount;
    }

    public int getAgodaAmenitiesCount() {
        return agodaAmenitiesCount;
    }

    public void setAgodaAmenitiesCount(int agodaAmenitiesCount) {
        this.agodaAmenitiesCount = agodaAmenitiesCount;
    }

    public List<String> getCommonAmenities() {
        return commonAmenities;
    }

    public void setCommonAmenities(List<String> commonAmenities) {
        this.commonAmenities = commonAmenities;
    }

    public List<String> getBookingOnlyAmenities() {
        return bookingOnlyAmenities;
    }

    public void setBookingOnlyAmenities(List<String> bookingOnlyAmenities) {
        this.bookingOnlyAmenities = bookingOnlyAmenities;
    }

    public List<String> getAgodaOnlyAmenities() {
        return agodaOnlyAmenities;
    }

    public void setAgodaOnlyAmenities(List<String> agodaOnlyAmenities) {
        this.agodaOnlyAmenities = agodaOnlyAmenities;
    }

    public String getRatingWinner() {
        return ratingWinner;
    }

    public void setRatingWinner(String ratingWinner) {
        this.ratingWinner = ratingWinner;
    }

    public String getAmenitiesWinner() {
        return amenitiesWinner;
    }

    public void setAmenitiesWinner(String amenitiesWinner) {
        this.amenitiesWinner = amenitiesWinner;
    }

    public List<String> getImprovementSuggestions() {
        return improvementSuggestions;
    }

    public void setImprovementSuggestions(List<String> improvementSuggestions) {
        this.improvementSuggestions = improvementSuggestions;
    }

    public String getOverallRecommendation() {
        return overallRecommendation;
    }

    public void setOverallRecommendation(String overallRecommendation) {
        this.overallRecommendation = overallRecommendation;
    }
}
