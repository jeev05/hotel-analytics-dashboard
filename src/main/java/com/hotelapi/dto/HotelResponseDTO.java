package com.hotelapi.dto;

import java.io.Serializable;

/**
 * API Response wrapper for hotel scraping results
 */
public class HotelResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String error;
    private HotelDTO hotel;
    private java.util.List<RoomOptionDTO> rooms;
    private CompetitorAnalysisDTO competitorAnalysis;
    private BookingVsAgodaComparisonDTO bookingVsAgodaComparison;
    private BookingAgodaComparisonDTO comparison;

    public HotelResponseDTO() {
    }

    public HotelResponseDTO(String error, HotelDTO hotel, 
                          java.util.List<RoomOptionDTO> rooms, 
                          CompetitorAnalysisDTO competitorAnalysis) {
        this.error = error;
        this.hotel = hotel;
        this.rooms = rooms;
        this.competitorAnalysis = competitorAnalysis;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public HotelDTO getHotel() {
        return hotel;
    }

    public void setHotel(HotelDTO hotel) {
        this.hotel = hotel;
    }

    public java.util.List<RoomOptionDTO> getRooms() {
        return rooms;
    }

    public void setRooms(java.util.List<RoomOptionDTO> rooms) {
        this.rooms = rooms;
    }

    public CompetitorAnalysisDTO getCompetitorAnalysis() {
        return competitorAnalysis;
    }

    public void setCompetitorAnalysis(CompetitorAnalysisDTO competitorAnalysis) {
        this.competitorAnalysis = competitorAnalysis;
    }

    public BookingVsAgodaComparisonDTO getBookingVsAgodaComparison() {
        return bookingVsAgodaComparison;
    }

    public void setBookingVsAgodaComparison(BookingVsAgodaComparisonDTO bookingVsAgodaComparison) {
        this.bookingVsAgodaComparison = bookingVsAgodaComparison;
    }

    public BookingAgodaComparisonDTO getComparison() {
        return comparison;
    }

    public void setComparison(BookingAgodaComparisonDTO comparison) {
        this.comparison = comparison;
    }
}
