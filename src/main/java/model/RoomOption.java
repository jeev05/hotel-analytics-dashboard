package model;

import java.util.ArrayList;
import java.util.List;

public class RoomOption {
	private String roomType = "N/A";
	private String guests = "N/A";
	private String price = "N/A";
	private String taxesAndFees = "N/A";
	private String availability = "N/A";
	private String roomSize = "N/A";
	private String bedInfo = "N/A";
	private final List<String> highlights = new ArrayList<>();
	private final List<String> amenities = new ArrayList<>();
	private final List<String> policies = new ArrayList<>();

	public String getRoomType() {
		return "N/A".equals(roomType) ? null : roomType;
	}

	public void setRoomType(String roomType) {
		this.roomType = roomType;
	}

	public String getGuests() {
		return "N/A".equals(guests) ? null : guests;
	}

	public void setGuests(String guests) {
		this.guests = guests;
	}

	public String getPrice() {
		return "N/A".equals(price) ? null : price;
	}

	public void setPrice(String price) {
		this.price = price;
	}

	public String getTaxesAndFees() {
		return "N/A".equals(taxesAndFees) ? null : taxesAndFees;
	}

	public void setTaxesAndFees(String taxesAndFees) {
		this.taxesAndFees = taxesAndFees;
	}

	public String getAvailability() {
		return "N/A".equals(availability) ? null : availability;
	}

	public void setAvailability(String availability) {
		this.availability = availability;
	}

	public String getRoomSize() {
		return "N/A".equals(roomSize) ? null : roomSize;
	}

	public void setRoomSize(String roomSize) {
		this.roomSize = roomSize;
	}

	public String getBedInfo() {
		return "N/A".equals(bedInfo) ? null : bedInfo;
	}

	public void setBedInfo(String bedInfo) {
		this.bedInfo = bedInfo;
	}

	public List<String> getHighlights() {
		return highlights;
	}
	public void setHighlights(List<String> highlights) {
    this.highlights.clear();
    if (highlights != null) {
        this.highlights.addAll(highlights);
    }
}

	public List<String> getAmenities() {
		return amenities;
	}
	public void setAmenities(List<String> amenities) {
    this.amenities.clear();
    if (amenities != null) {
        this.amenities.addAll(amenities);
    }
}

public void setPolicies(List<String> policies) {
    this.policies.clear();
    if (policies != null) {
        this.policies.addAll(policies);
    }
}

	public List<String> getPolicies() {
		return policies;
	}

	public String toDisplayString(int index) {
		StringBuilder builder = new StringBuilder();
		builder.append("Room ").append(index).append(':').append('\n');
		builder.append("  Type: ").append(getRoomType() == null ? "Data unavailable" : getRoomType()).append('\n');
		builder.append("  Guests: ").append(getGuests() == null ? "Data unavailable" : getGuests()).append('\n');
		builder.append("  Price: ").append(getPrice() == null ? "Data unavailable" : getPrice()).append('\n');
		builder.append("  Taxes and Fees: ").append(getTaxesAndFees() == null ? "Data unavailable" : getTaxesAndFees()).append('\n');
		builder.append("  Availability: ").append(getAvailability() == null ? "Data unavailable" : getAvailability()).append('\n');
		builder.append("  Room Size: ").append(getRoomSize() == null ? "Data unavailable" : getRoomSize()).append('\n');
		builder.append("  Bed Info: ").append(getBedInfo() == null ? "Data unavailable" : getBedInfo()).append('\n');
		appendList(builder, "  Highlights", highlights);
		appendList(builder, "  Amenities", amenities);
		appendList(builder, "  Policies", policies);
		return builder.toString();
	}

	private void appendList(StringBuilder builder, String title, List<String> values) {
		builder.append(title).append(':').append('\n');
		if (values == null || values.isEmpty()) {
			builder.append("    - Data unavailable\n");
			return;
		}
		for (String value : values) {
			builder.append("    - ").append(value == null ? "Data unavailable" : value).append('\n');
		}
	}
}