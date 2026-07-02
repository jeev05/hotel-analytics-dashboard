package model;

import java.util.ArrayList;
import java.util.List;

public class CompetitorAnalysis {
    private List<String> missingAmenities = new ArrayList<>();
    private String priceComparison = "N/A";
    private String ratingComparison = "N/A";
    private String reviewComparison = "N/A";
    private List<String> strengths = new ArrayList<>();
    private List<String> weaknesses = new ArrayList<>();
    private List<String> recommendations = new ArrayList<>();

    public List<String> getMissingAmenities() {
        return missingAmenities;
    }

    public void setMissingAmenities(List<String> missingAmenities) {
        this.missingAmenities = missingAmenities;
    }

    public String getPriceComparison() {
        return priceComparison;
    }

    public void setPriceComparison(String priceComparison) {
        this.priceComparison = priceComparison;
    }

    public String getRatingComparison() {
        return ratingComparison;
    }

    public void setRatingComparison(String ratingComparison) {
        this.ratingComparison = ratingComparison;
    }

    public String getReviewComparison() {
        return reviewComparison;
    }

    public void setReviewComparison(String reviewComparison) {
        this.reviewComparison = reviewComparison;
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

    public String toDisplayString() {
        StringBuilder sb = new StringBuilder();
        sb.append("COMPETITOR ANALYSIS\n");
        sb.append("--------------------\n");
        sb.append("Price Comparison: ").append(priceComparison).append("\n");
        sb.append("Rating Comparison: ").append(ratingComparison).append("\n");
        sb.append("Review Comparison: ").append(reviewComparison).append("\n");
        sb.append("Missing Amenities: \n");
        if (missingAmenities.isEmpty()) {
            sb.append("- None\n");
        } else {
            for (String amenity : missingAmenities) {
                sb.append("- ").append(amenity).append("\n");
            }
        }

        sb.append("Strengths: \n");
        if (strengths.isEmpty()) {
            sb.append("- None\n");
        } else {
            for (String strength : strengths) {
                sb.append("- ").append(strength).append("\n");
            }
        }

        sb.append("Weaknesses: \n");
        if (weaknesses.isEmpty()) {
            sb.append("- None\n");
        } else {
            for (String weakness : weaknesses) {
                sb.append("- ").append(weakness).append("\n");
            }
        }

        sb.append("Recommendations: \n");
        if (recommendations.isEmpty()) {
            sb.append("- None\n");
        } else {
            for (String recommendation : recommendations) {
                sb.append("- ").append(recommendation).append("\n");
            }
        }
        return sb.toString();
    }
}
