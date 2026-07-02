const reviewSentimentMap = [
  { pattern: /excellent/i, label: 'Excellent' },
  { pattern: /very good|very-good/i, label: 'Very Good' },
  { pattern: /good/i, label: 'Good' },
  { pattern: /fair/i, label: 'Fair' },
  { pattern: /poor/i, label: 'Poor' }
];

const amenityBlacklist = [
  /\broom\b/i,
  /\broom size\b/i,
  /\broom area\b/i,
  /\bmax people\b/i,
  /\bpeople\b/i,
  /\bguests?\b/i,
  /\bmeal plan\b/i,
  /\bm²\b/i,
  /\bmeters?\b/i,
  /\bsize\b/i,
  /\bmin\b/i,
  /\bheight\b/i,
  /\bwidth\b/i,
  /\blong\b/i,
  /\bkg\b/i,
  /\bmi\b/i,
  /\bfeet?\b/i,
  /\barea\b/i,
  /\broom\b/i
];

export function parsePrice(text) {
  if (!text) {
    return null;
  }
  const normalized = String(text).replace(/\s+/g, ' ').trim();
  const match = normalized.match(/([₹$€£]|USD|INR|EUR|GBP)?\s*([0-9]{1,3}(?:[.,][0-9]{3})*(?:\.[0-9]+)?|[0-9]+(?:\.[0-9]+)?)/i);
  if (!match) {
    return null;
  }
  const currency = match[1] ? match[1].replace(/USD/i, '$').replace(/INR/i, '₹').replace(/EUR/i, '€').replace(/GBP/i, '£') : '';
  const amount = match[2].replace(/,/g, '');
  return `${currency}${Number(amount).toLocaleString('en-IN')}`;
}

export function parseRating(text) {
  if (!text) {
    return null;
  }
  const normalized = String(text).replace(/,/g, '.').trim();
  const match = normalized.match(/([0-9]+(?:\.[0-9]+)?)/);
  return match ? Number(match[1]).toFixed(1) : null;
}

export function parseReviewCount(text) {
  if (!text) {
    return null;
  }
  const match = String(text).replace(/,/g, '').match(/([0-9]{1,6})/);
  return match ? Number(match[1]) : null;
}

export function parseSentiment(text) {
  if (!text) {
    return null;
  }
  const normalized = String(text);
  for (const item of reviewSentimentMap) {
    if (item.pattern.test(normalized)) {
      return item.label;
    }
  }
  return null;
}

export function parseRoomType(text) {
  if (!text) {
    return null;
  }

  const normalized = String(text)
    .replace(/\s*\|\s*/g, ' - ')
    .replace(/\s*\(.*\)\s*/g, '')
    .replace(/\bwith\b.*$/i, '')
    .replace(/\bper night before taxes\b/i, '')
    .replace(/\boriginal price\b/i, '')
    .trim();

  if (!normalized || normalized.length < 3) {
    return null;
  }

  const noisePatterns = [
    /we price match/i,
    /breakfast included/i,
    /filter/i,
    /parking/i,
    /see details/i,
    /original price/i,
    /per night/i,
    /cancellation policy/i,
    /offer\s*\d+/i,
    /city view/i,
    /reviews?/i,
    /location/i,
    /policies?/i,
    /tooltip/i,
    /hotel name/i,
    /address/i,
    /price/i,
    /marketing/i,
    /upload/i,
    /recommendation/i,
    /book now/i,
    /suite selection/i,
    /room size/i,
    /guests?/i,
    /adults?/i,
    /people/i
  ];

  if (noisePatterns.some((pattern) => pattern.test(normalized))) {
    return null;
  }

  return normalized;
}

function normalizeComparableName(text) {
  if (!text) {
    return '';
  }

  return String(text)
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

export function parseGuests(text) {
  if (!text) {
    return null;
  }
  const normalized = String(text);
  const match = normalized.match(/(\d+)\s*(?:guests?|persons?|people|adults?)/i);
  if (match) {
    return match[1];
  }
  const simpleMatch = normalized.match(/^(\d+)$/);
  return simpleMatch ? simpleMatch[1] : normalized;
}

export function parseBooleanFlag(text, truthyPatterns = []) {
  if (!text) {
    return false;
  }
  const normalized = String(text).toLowerCase();
  if (/no|not|excluded|unavailable|sold out|sold-out|non-refundable|non refundable/i.test(normalized)) {
    return false;
  }
  if (/yes|included|free|available|refundable|included/i.test(normalized)) {
    return true;
  }
  return truthyPatterns.some((pattern) => pattern.test(normalized));
}

export function parseAvailability(text) {
  if (!text) {
    return 'Unknown';
  }
  const normalized = String(text).trim();
  if (/sold out|unavailable|not available/i.test(normalized)) {
    return 'Unavailable';
  }
  if (/available|rooms left|left|available/i.test(normalized)) {
    return 'Available';
  }
  return normalized;
}

export function parseAmenities(values) {
  if (!values) {
    return [];
  }
  const list = Array.isArray(values) ? values : String(values).split(/[,;|\n]/g);
  return Array.from(
    new Set(
      list
        .map((item) => String(item).trim())
        .filter((item) => item && item.length > 2)
        .filter((item) => !/(reviews?|location|policies?|tooltip|hotel name|address|price|marketing|offer|cancellation|per night|see details|recommendation|upload|we price match|filter)/i.test(item))
        .filter((item) => !amenityBlacklist.some((pattern) => pattern.test(item)))
    )
  );
}

export function parsePolicies(values) {
  if (!values) {
    return [];
  }
  const list = Array.isArray(values) ? values : String(values).split(/[,;|\n]/g);
  return Array.from(
    new Set(
      list
        .map((item) => String(item).trim())
        .filter((item) => item)
        .filter((item) => !/\b(reviews?|location|tooltip|hotel name|address|price|marketing|offer|cancellation|per night|see details|recommendation|upload|home|hotels|india|tamil nadu|overview|facilities|breadcrumb|back to|previous page|next page|top nav|search results|about us|about this property)\b/i.test(item))
        .filter((item) => !/^(no policies found|none|n\/a)$/i.test(item))
    )
  );
}

// House rules are structured "Title : Value" lines (check-in, check-out, cancellation/prepayment,
// children & beds, pets, cards accepted, etc.) scraped from the listing's "House rules" section.
// They need lighter filtering than parsePolicies, since legitimate rules legitimately mention
// words like "cancellation" or "price" that parsePolicies strips out as navigation noise.
export function parseHouseRules(values) {
  if (!values) {
    return [];
  }
  const list = Array.isArray(values) ? values : String(values).split(/[\n]/g);
  return Array.from(
    new Set(
      list
        .map((item) => String(item).replace(/\s+/g, ' ').trim())
        .filter((item) => item && item.length > 2)
        .filter((item) => !/^(no policies found|none|n\/a|house rules)$/i.test(item))
        .filter((item) => !/\b(tooltip|breadcrumb|back to|previous page|next page|top nav|search results|about us|see availability)\b/i.test(item))
    )
  );
}

export function parseRoomData(room) {
  if (!room) {
    return null;
  }
  const roomType = parseRoomType(room.roomType || room.title || room.name || '');
  if (!roomType) {
    return null;
  }

  return {
    roomType,
    guests: parseGuests(room.guests || room.occupancy || ''),
    price: parsePrice(room.price || room.rate || ''),
    breakfastIncluded: parseBooleanFlag(room.highlights?.join(' ') || room.policies?.join(' ') || room.breakfast || ''),
    refundable: parseBooleanFlag(room.policies?.join(' ') || room.refundable || '', [/refundable/i]),
    availability: parseAvailability(room.availability || room.status || ''),
    rawHighlights: room.highlights || [],
    rawPolicies: room.policies || []
  };
}

export function parseCompetitorData(data, selectedHotelName) {
  if (!data) {
    return [];
  }
  if (Array.isArray(data)) {
    return data.flatMap((item) => parseCompetitorData(item, selectedHotelName));
  }
  if (typeof data === 'object') {
    if (Array.isArray(data.hotels)) {
      const candidates = data.hotels.map((hotel) => ({
        name: hotel.name || hotel.hotel || 'Competitor',
        price: parsePrice(hotel.price || hotel.rate || ''),
        rating: parseRating(hotel.rating || hotel.reviewScore || ''),
        reviewCount: parseReviewCount(hotel.reviews || hotel.reviewCount || '')
      }));

      if (!selectedHotelName) {
        return candidates;
      }

      const normalizedSelected = normalizeComparableName(selectedHotelName);
      return candidates.filter((item) => {
        if (!item.name) return false;
        const normalized = normalizeComparableName(item.name);
        return normalized && normalized !== normalizedSelected && !normalized.includes(normalizedSelected) && !normalizedSelected.includes(normalized);
      });
    }

    if (data.priceComparison || data.ratingComparison) {
      const priceComparison = data.priceComparison || {};
      const ratingComparison = data.ratingComparison || {};
      const reviewComparison = data.reviewCounts || data.reviews || {};

      return Object.entries(priceComparison)
        .map(([name, price]) => ({
          name,
          price: parsePrice(price || ''),
          rating: parseRating(ratingComparison?.[name] || ''),
          reviewCount: parseReviewCount(reviewComparison?.[name] || '')
        }))
        .filter((item) => {
          if (!item.name) return false;
          if (!selectedHotelName) return true;
          const normalized = normalizeComparableName(item.name);
          const selected = normalizeComparableName(selectedHotelName);
          return normalized && selected && normalized !== selected && !normalized.includes(selected) && !selected.includes(normalized);
        });
    }

    return [{
      name: data.hotelName || data.name || 'Competitor',
      price: parsePrice(data.price || ''),
      rating: parseRating(data.rating || ''),
      reviewCount: parseReviewCount(data.reviews || '')
    }];
  }
  return [];
}

export function parseAddress(text) {
  if (!text) {
    return null;
  }
  const normalized = String(text).replace(/\s+/g, ' ').trim();
  if (!normalized || /\b(address unavailable|hotel name|see map|location details?|map|tooltip)\b/i.test(normalized)) {
    return null;
  }
  return normalized.replace(/^address\s*[:\-]?\s*/i, '').trim();
}

export function normalizeHotelData(response) {
  console.log('Normalizing hotel response', response);
  const hotel = response.hotel || {};
  const roomOptions = Array.isArray(response.rooms) ? response.rooms.map(parseRoomData).filter(Boolean) : [];
  const amenities = parseAmenities(hotel.amenities || hotel.description || hotel.additionalInfo || []);
  const policies = parsePolicies(hotel.policies || hotel.policy || roomOptions.flatMap((room) => room.rawPolicies || []));
  const houseRules = parseHouseRules(hotel.houseRules || hotel.houseRule || []);
  const rating = parseRating(hotel.rating || hotel.reviewScore || hotel.ratingScore || '');
  const reviewCount = parseReviewCount(hotel.reviews || hotel.reviewCount || '');
  const sentiment = parseSentiment(hotel.reviews || hotel.rating || hotel.description || '');
  const price = parsePrice(hotel.price || hotel.rate || hotel.roomPrice || '');
  const address = parseAddress(hotel.address || hotel.location || hotel.propertyAddress || '');

  return {
    hotel: {
      ...hotel,
      price: price || null,
      rating: rating || null,
      address: address || null,
      sentiment,
      reviews: reviewCount || null,
      amenities,
      policies,
      houseRules,
      imageScore: typeof hotel.imageScore === 'number' ? hotel.imageScore : parseRating(hotel.imageScore),
      blurry: hotel.blurry || false,
      reviewLabel: sentiment || 'N/A'
    },
    rooms: roomOptions,
    competitors: parseCompetitorData(response.competitorAnalysis, hotel.name)
  };
}

// Expose Booking/Agoda comparison into normalized hotel shape when backend provides it
export function attachComparison(normalized, response) {
  if (!normalized || !response) return normalized;
  const comparison = response.comparison || response.bookingAgodaComparison || response.bookingVsAgodaComparison;
  if (!comparison) return normalized;

  normalized.hotel = {
    ...normalized.hotel,
    booking: {
      price: comparison.bookingPrice || null,
      rating: comparison.bookingRating || null,
      reviewCount: comparison.bookingReviewCount || null,
      amenitiesCount: comparison.bookingAmenitiesCount || null
    },
    agoda: {
      price: comparison.agodaPrice || null,
      rating: comparison.agodaRating || null,
      reviewCount: comparison.agodaReviewCount || null,
      amenitiesCount: comparison.agodaAmenitiesCount || null
    },
    comparison: comparison
  };
  return normalized;
}
