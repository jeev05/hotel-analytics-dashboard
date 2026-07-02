package com.hotelapi.dto;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Competitor analysis data transfer object for API responses
 */
public class CompetitorAnalysisDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private Map<String, String> priceComparison;
    private Map<String, String> ratingComparison;
    private Double selectedHotelPrice;
    private Double averageCompetitorPrice;
    private Double selectedHotelRating;
    private Double averageCompetitorRating;
    private String pricingInsight;
    private String ratingInsight;
    private List<String> missingAmenities;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> recommendations;

    public CompetitorAnalysisDTO() {
    }

    public CompetitorAnalysisDTO(Map<String, String> priceComparison, Map<String, String> ratingComparison,
                                List<String> missingAmenities, List<String> strengths,
                                List<String> weaknesses, List<String> recommendations) {
        this.priceComparison = priceComparison;
        this.ratingComparison = ratingComparison;
        this.missingAmenities = missingAmenities;
        this.strengths = strengths;
        this.weaknesses = weaknesses;
        this.recommendations = recommendations;
    }

    // Getters and Setters
    public Map<String, String> getPriceComparison() {
        return priceComparison;
    }

    public void setPriceComparison(Map<String, String> priceComparison) {
        this.priceComparison = priceComparison;
    }

    public Map<String, String> getRatingComparison() {
        return ratingComparison;
    }

    public void setRatingComparison(Map<String, String> ratingComparison) {
        this.ratingComparison = ratingComparison;
    }

    public Double getSelectedHotelPrice() {
        return selectedHotelPrice;
    }

    public void setSelectedHotelPrice(Double selectedHotelPrice) {
        this.selectedHotelPrice = selectedHotelPrice;
    }

    public Double getAverageCompetitorPrice() {
        return averageCompetitorPrice;
    }

    public void setAverageCompetitorPrice(Double averageCompetitorPrice) {
        this.averageCompetitorPrice = averageCompetitorPrice;
    }

    public Double getSelectedHotelRating() {
        return selectedHotelRating;
    }

    public void setSelectedHotelRating(Double selectedHotelRating) {
        this.selectedHotelRating = selectedHotelRating;
    }

    public Double getAverageCompetitorRating() {
        return averageCompetitorRating;
    }

    public void setAverageCompetitorRating(Double averageCompetitorRating) {
        this.averageCompetitorRating = averageCompetitorRating;
    }

    public String getPricingInsight() {
        return pricingInsight;
    }

    public void setPricingInsight(String pricingInsight) {
        this.pricingInsight = pricingInsight;
    }

    public String getRatingInsight() {
        return ratingInsight;
    }

    public void setRatingInsight(String ratingInsight) {
        this.ratingInsight = ratingInsight;
    }

    public List<String> getMissingAmenities() {
        return missingAmenities;
    }

    public void setMissingAmenities(List<String> missingAmenities) {
        this.missingAmenities = missingAmenities;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public void setStrengths(List<String> strengths) {
        this.strengths = strengths;
    }

    public List<String> getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(List<String> weaknesses) {
        this.weaknesses = weaknesses;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }
}
