package com.hotelapi.dto;

import java.io.Serializable;
import java.util.List;

/**
 * Room option data transfer object for API responses
 */
public class RoomOptionDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String roomType;
    private String guests;
    private String price;
    private String taxesAndFees;
    private String availability;
    private String roomSize;
    private String bedInfo;
    private List<String> highlights;
    private List<String> amenities;
    private List<String> policies;

    public RoomOptionDTO() {
    }

    // Getters and Setters
    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public String getGuests() {
        return guests;
    }

    public void setGuests(String guests) {
        this.guests = guests;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getTaxesAndFees() {
        return taxesAndFees;
    }

    public void setTaxesAndFees(String taxesAndFees) {
        this.taxesAndFees = taxesAndFees;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public String getRoomSize() {
        return roomSize;
    }

    public void setRoomSize(String roomSize) {
        this.roomSize = roomSize;
    }

    public String getBedInfo() {
        return bedInfo;
    }

    public void setBedInfo(String bedInfo) {
        this.bedInfo = bedInfo;
    }

    public List<String> getHighlights() {
        return highlights;
    }

    public void setHighlights(List<String> highlights) {
        this.highlights = highlights;
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
}
