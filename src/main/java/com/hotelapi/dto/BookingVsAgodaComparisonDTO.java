package com.hotelapi.dto;

import java.io.Serializable;
import java.util.List;

public class BookingVsAgodaComparisonDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Double bookingRating;
    private Double agodaRating;
    private Integer bookingAmenitiesCount;
    private Integer agodaAmenitiesCount;
    private List<String> commonAmenities;
    private List<String> bookingOnlyAmenities;
    private List<String> agodaOnlyAmenities;
    private String ratingWinner;
    private String amenitiesWinner;
    private String overallRecommendation;

    public BookingVsAgodaComparisonDTO() {
    }

    public Double getBookingRating() {
        return bookingRating;
    }

    public void setBookingRating(Double bookingRating) {
        this.bookingRating = bookingRating;
    }

    public Double getAgodaRating() {
        return agodaRating;
    }

    public void setAgodaRating(Double agodaRating) {
        this.agodaRating = agodaRating;
    }

    public Integer getBookingAmenitiesCount() {
        return bookingAmenitiesCount;
    }

    public void setBookingAmenitiesCount(Integer bookingAmenitiesCount) {
        this.bookingAmenitiesCount = bookingAmenitiesCount;
    }

    public Integer getAgodaAmenitiesCount() {
        return agodaAmenitiesCount;
    }

    public void setAgodaAmenitiesCount(Integer agodaAmenitiesCount) {
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

    public String getOverallRecommendation() {
        return overallRecommendation;
    }

    public void setOverallRecommendation(String overallRecommendation) {
        this.overallRecommendation = overallRecommendation;
    }
}
