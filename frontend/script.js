// ========== API CONFIGURATION ==========
// ========== API CONFIGURATION ==========
const API_BASE_URLS = [
    'https://hotel-analytics-api.onrender.com/api'
];

const TIMEOUT_MS = 120000; // 120 second timeout for scraping calls // 120 second timeout for scraping calls

// ========== DOM ELEMENTS ==========
const elements = {
    hotelInput: document.getElementById('hotelInput'),
    sourceSelect: document.getElementById('sourceSelect'),
    apiStatus: document.getElementById('apiStatus'),
    loading: document.getElementById('loading'),
    error: document.getElementById('error'),
    results: document.getElementById('results'),
    emptyState: document.getElementById('emptyState'),
    
    // Hotel Details
    hotelTitle: document.getElementById('hotelTitle'),
    hotelAddress: document.getElementById('hotelAddress'),
    hotelPrice: document.getElementById('hotelPrice'),
    hotelRating: document.getElementById('hotelRating'),
    hotelReviews: document.getElementById('hotelReviews'),
    sourceBadge: document.getElementById('sourceBadge'),
    amenitiesList: document.getElementById('amenitiesList'),
    imageQuality: document.getElementById('imageQuality'),
    imageStatus: document.getElementById('imageStatus'),
    
    // Rooms
    roomsContainer: document.getElementById('roomsContainer'),
    
    // Competitor Analysis
    priceComparison: document.getElementById('priceComparison'),
    ratingComparison: document.getElementById('ratingComparison'),
    strengthsList: document.getElementById('strengthsList'),
    weaknessesList: document.getElementById('weaknessesList'),
    recommendationsList: document.getElementById('recommendationsList'),
};

let activeApiBaseUrl = null;

// ========== MAIN ANALYSIS FUNCTION ==========
async function analyzeHotel() {
    const hotelName = elements.hotelInput.value.trim();
    const source = (elements.sourceSelect?.value || 'booking').toLowerCase();
    
    if (!hotelName) {
        showError('Please enter a hotel name');
        return;
    }
    
    try {
        showLoading(true);
        clearError();

        await ensureBackendConnection();
        
        // Fetch hotel data from backend
        const hotelData = await fetchHotelData(hotelName, source);
        
        // Display results
        displayResults(hotelData, source);
        showResults(true);
        
    } catch (error) {
        console.error('Error analyzing hotel:', error);
        let errorMessage = error.message;
        
        // Provide helpful suggestions for common errors
        if (errorMessage.includes('404') && errorMessage.includes('Hotel not found')) {
            errorMessage += '\n\n💡 Try these popular hotels:\n• Taj Mahal Palace Mumbai\n• ITC Grand Chola Chennai\n• The Leela Palace New Delhi\n• Oberoi Mumbai\n• Park Plaza London';
        }
        
        showError(`Error: ${errorMessage}`);
        showResults(false);
    } finally {
        showLoading(false);
    }
}

// ========== API CALL FUNCTION ==========
async function fetchHotelData(hotelName, source) {
    let lastError;
    let lastHttpError;

    const urlsToTry = activeApiBaseUrl
        ? [activeApiBaseUrl, ...API_BASE_URLS.filter(url => url !== activeApiBaseUrl)]
        : API_BASE_URLS;

    for (const baseUrl of urlsToTry) {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), TIMEOUT_MS);

        try {
            const response = await fetch(`${baseUrl}/scrape?hotelName=${encodeURIComponent(hotelName)}&source=${encodeURIComponent(source)}`, {
                method: 'GET',
                signal: controller.signal
            });

            clearTimeout(timeoutId);

            if (!response.ok) {
                const errorBody = await response.text();
                const suffix = errorBody ? ` - ${errorBody}` : '';
                lastHttpError = new Error(`API Error from ${baseUrl}: ${response.status} ${response.statusText}${suffix}`);
                continue;
            }

            const data = await response.json();
            activeApiBaseUrl = baseUrl;
            setApiStatus(`Connected to ${baseUrl}`, true);
            return data;
        } catch (error) {
            clearTimeout(timeoutId);
            lastError = error;
        }
    }

    if (lastHttpError) {
        throw lastHttpError;
    }

    if (lastError?.name === 'AbortError') {
        throw new Error('Request timeout. The server took too long to respond.');
    }

    const detail = lastError?.message ? ` Last error: ${lastError.message}` : '';
    throw new Error(`Failed to reach backend API. Ensure backend is running on port 9090 and CORS allows this frontend origin.${detail}`);
}

async function ensureBackendConnection() {
    const urlsToTry = activeApiBaseUrl
        ? [activeApiBaseUrl, ...API_BASE_URLS.filter(url => url !== activeApiBaseUrl)]
        : API_BASE_URLS;

    for (const baseUrl of urlsToTry) {
        try {
            const response = await fetch(`${baseUrl}/health`, { method: 'GET' });
            if (response.ok) {
                activeApiBaseUrl = baseUrl;
                setApiStatus(`Connected to ${baseUrl}`, true);
                return;
            }
        } catch (error) {
            // Try the next URL.
        }
    }

    activeApiBaseUrl = null;
    setApiStatus('Backend disconnected. Start API on port 9090.', false);
}

function setApiStatus(message, isConnected) {
    if (!elements.apiStatus) {
        return;
    }
    elements.apiStatus.textContent = message;
    elements.apiStatus.classList.remove('connected', 'disconnected');
    elements.apiStatus.classList.add(isConnected ? 'connected' : 'disconnected');
}

// ========== DISPLAY FUNCTIONS ==========
function displayResults(data, source) {
    // Display hotel details
    displayHotelDetails(data.hotel || {}, source);
    
    // Display room options
    displayRoomOptions(data.rooms || []);
    
    // Display competitor analysis
    displayCompetitorAnalysis(data.competitorAnalysis || {});
}

function displayHotelDetails(hotel, source) {
    const sourceLabel = source === 'agoda' ? 'Agoda' : 'Booking.com';
    if (elements.sourceBadge) {
        elements.sourceBadge.textContent = `Source: ${sourceLabel}`;
    }

    elements.hotelTitle.textContent = hotel.name || '—';
    elements.hotelAddress.textContent = hotel.address || 'No data available';
    elements.hotelPrice.textContent = formatPrice(hotel.price) || '—';
    elements.hotelRating.textContent = formatRating(hotel.rating) || '—';
    elements.hotelReviews.textContent = hotel.reviews ? `(${hotel.reviews} reviews)` : '(—)';
    
    // Display image quality score
    if (elements.imageQuality) {
        const score = hotel.imageScore?.toFixed(2) || '0.00';
        const scorePercentage = (hotel.imageScore || 0).toFixed(1);
        elements.imageQuality.textContent = `${score}/100 (${scorePercentage}%)`;
    }
    
    // Display image status (blurry or clear)
    if (elements.imageStatus) {
        const status = hotel.blurry ? '⚠️ Blurry Image' : '✓ Clear Image';
        elements.imageStatus.textContent = status;
    }
    
    // Display amenities
    if (hotel.amenities && hotel.amenities.length > 0) {
        elements.amenitiesList.innerHTML = hotel.amenities
            .map(amenity => `<span class="tag">${escapeHtml(amenity)}</span>`)
            .join('');
    } else {
        elements.amenitiesList.innerHTML = '<span class="tag">No amenities data available</span>';
    }
}

function displayRoomOptions(rooms) {
    if (!rooms || rooms.length === 0) {
        elements.roomsContainer.innerHTML = '<p class="no-data">No room options available</p>';
        return;
    }
    
    elements.roomsContainer.innerHTML = rooms.map(room => `
        <div class="room-card">
            <h4>${escapeHtml(room.roomType || 'Room')}</h4>
            <div class="room-info">
                <div class="room-row">
                    <span class="room-label">Price</span>
                    <span class="room-value room-price">${formatPrice(room.price) || '—'}</span>
                </div>
                <div class="room-row">
                    <span class="room-label">Guests</span>
                    <span class="room-value">${room.guests || '—'}</span>
                </div>
                <div class="room-row">
                    <span class="room-label">Availability</span>
                    <span class="room-value">${room.availability || '—'}</span>
                </div>
                ${room.policies ? `
                <div class="room-row">
                    <span class="room-label">Policies</span>
                    <span class="room-value">${escapeHtml(room.policies)}</span>
                </div>
                ` : ''}
            </div>
        </div>
    `).join('');
}

function displayCompetitorAnalysis(analysis) {
    const priceMap = analysis.priceComparison || {};
    const ratingMap = analysis.ratingComparison || {};

    const priceSeries = Object.entries(priceMap)
        .map(([name, value]) => ({
            name,
            raw: value,
            numeric: parseNumericFromText(value)
        }))
        .filter(item => !Number.isNaN(item.numeric));

    const ratingSeries = Object.entries(ratingMap)
        .map(([name, value]) => ({
            name,
            raw: value,
            numeric: parseNumericFromText(value)
        }))
        .filter(item => !Number.isNaN(item.numeric));

    // Price Comparison with visual bars
    if (priceSeries.length > 0) {
        const maxPrice = Math.max(...priceSeries.map(x => x.numeric));
        const avgPrice = analysis.averageCompetitorPrice;
        const selectedPrice = analysis.selectedHotelPrice;
        const summary = Number.isFinite(avgPrice) && Number.isFinite(selectedPrice)
            ? `<div class="metric-summary">Main: ${formatPrice(selectedPrice)} | Nearby Avg: ${formatPrice(avgPrice)}</div>`
            : '';

        elements.priceComparison.innerHTML = `
            ${summary}
            ${priceSeries.map(item => {
                const ratio = maxPrice > 0 ? (item.numeric / maxPrice) * 100 : 0;
                return `
                    <div class="metric-row">
                        <div class="metric-label-line">
                            <span class="platform-name">${escapeHtml(item.name)}</span>
                            <span class="platform-value">${escapeHtml(item.raw)}</span>
                        </div>
                        <div class="metric-bar-track">
                            <div class="metric-bar price-bar" style="width:${Math.max(6, ratio).toFixed(1)}%"></div>
                        </div>
                    </div>
                `;
            }).join('')}
            ${analysis.pricingInsight ? `<div class="insight-chip">${escapeHtml(analysis.pricingInsight)}</div>` : ''}
        `;
    } else {
        elements.priceComparison.innerHTML = '<p class="no-data">No price data available</p>';
    }

    // Rating Comparison with visual bars
    if (ratingSeries.length > 0) {
        const maxRating = Math.max(...ratingSeries.map(x => x.numeric), 5);
        const avgRating = analysis.averageCompetitorRating;
        const selectedRating = analysis.selectedHotelRating;
        const summary = Number.isFinite(avgRating) && Number.isFinite(selectedRating)
            ? `<div class="metric-summary">Main: ${formatRating(selectedRating)} | Nearby Avg: ${avgRating.toFixed(2)} ⭐</div>`
            : '';

        elements.ratingComparison.innerHTML = `
            ${summary}
            ${ratingSeries.map(item => {
                const ratio = maxRating > 0 ? (item.numeric / maxRating) * 100 : 0;
                return `
                    <div class="metric-row">
                        <div class="metric-label-line">
                            <span class="platform-name">${escapeHtml(item.name)}</span>
                            <span class="platform-value">${escapeHtml(item.raw)}</span>
                        </div>
                        <div class="metric-bar-track">
                            <div class="metric-bar rating-bar" style="width:${Math.max(6, ratio).toFixed(1)}%"></div>
                        </div>
                    </div>
                `;
            }).join('')}
            ${analysis.ratingInsight ? `<div class="insight-chip">${escapeHtml(analysis.ratingInsight)}</div>` : ''}
        `;
    } else {
        elements.ratingComparison.innerHTML = '<p class="no-data">No rating data available</p>';
    }
    
    // Strengths
    if (analysis.strengths && analysis.strengths.length > 0) {
        elements.strengthsList.innerHTML = analysis.strengths
            .map(strength => `<li>${escapeHtml(strength)}</li>`)
            .join('');
    } else {
        elements.strengthsList.innerHTML = '<li class="no-data">No data available</li>';
    }
    
    // Weaknesses
    if (analysis.weaknesses && analysis.weaknesses.length > 0) {
        elements.weaknessesList.innerHTML = analysis.weaknesses
            .map(weakness => `<li>${escapeHtml(weakness)}</li>`)
            .join('');
    } else {
        elements.weaknessesList.innerHTML = '<li class="no-data">No data available</li>';
    }
    
    // Recommendations
    if (analysis.recommendations && analysis.recommendations.length > 0) {
        elements.recommendationsList.innerHTML = analysis.recommendations
            .map(rec => `
                <div class="recommendation-item">
                    <strong>${escapeHtml(rec.title || 'Recommendation')}</strong>
                    <p>${escapeHtml(rec.description || rec)}</p>
                </div>
            `)
            .join('');
    } else {
        elements.recommendationsList.innerHTML = '<p class="no-data">No recommendations available</p>';
    }

    if (analysis.missingAmenities && analysis.missingAmenities.length > 0) {
        elements.recommendationsList.innerHTML += `
            <div class="recommendation-item warning">
                <strong>Missing Key Amenities</strong>
                <p>${analysis.missingAmenities.map(escapeHtml).join(', ')}</p>
            </div>
        `;
    }
}

function parseNumericFromText(value) {
    if (value === null || value === undefined) {
        return Number.NaN;
    }
    if (typeof value === 'number') {
        return value;
    }
    const match = String(value).match(/\d+(?:\.\d+)?/);
    return match ? Number.parseFloat(match[0]) : Number.NaN;
}

// ========== UI STATE FUNCTIONS ==========
function showLoading(show) {
    elements.loading.classList.toggle('hidden', !show);
}

function showResults(show) {
    elements.results.classList.toggle('hidden', !show);
    elements.emptyState.classList.toggle('hidden', show);
}

function showError(message) {
    elements.error.textContent = message;
    elements.error.classList.remove('hidden');
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function clearError() {
    elements.error.classList.add('hidden');
    elements.error.textContent = '';
}

// ========== UTILITY FUNCTIONS ==========
function formatPrice(price) {
    if (!price || price === '—') return '—';
    if (typeof price === 'string') return price;
    
    // Format as currency
    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
    }).format(price);
}

function formatRating(rating) {
    if (!rating || rating === '—') return '—';
    if (typeof rating === 'number') {
        return rating.toFixed(1) + ' ⭐';
    }
    return rating;
}

function escapeHtml(text) {
    if (!text) return '';
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.toString().replace(/[&<>"']/g, m => map[m]);
}

// ========== EVENT LISTENERS ==========
function handleKeyPress(event) {
    if (event.key === 'Enter') {
        analyzeHotel();
    }
}

// Initialize on page load
document.addEventListener('DOMContentLoaded', function() {
    // Remove loading state on initial load
    showLoading(false);
    showResults(false);
    
    // Focus on input
    elements.hotelInput.focus();

    // Initial backend connectivity check
    ensureBackendConnection();
});

// ========== MOCK DATA FOR TESTING ==========
// Uncomment below to test UI without backend
/*
function testWithMockData() {
    const mockData = {
        hotel: {
            name: "Taj Hotel Mumbai",
            address: "123 Marine Drive, Mumbai, India",
            price: 15000,
            rating: 4.5,
            reviews: 2847,
            amenities: ["Free WiFi", "Swimming Pool", "Gym", "Restaurant", "Room Service", "Air Conditioning"]
        },
        rooms: [
            {
                roomType: "Standard Room",
                price: 12000,
                guests: "1-2",
                availability: "Available",
                policies: "Free Cancellation"
            },
            {
                roomType: "Deluxe Room",
                price: 15000,
                guests: "2-3",
                availability: "Available",
                policies: "2 days advance payment"
            }
        ],
        competitorAnalysis: {
            priceComparison: {
                "Booking.com": 15000,
                "Agoda": 14500,
                "MakeMyTrip": 15500
            },
            priceDifference: {
                "Booking.com": 0,
                "Agoda": -500,
                "MakeMyTrip": 500
            },
            ratingComparison: {
                "Booking.com": 4.5,
                "Agoda": 4.3,
                "MakeMyTrip": 4.2
            },
            strengths: [
                "Consistently high ratings across all platforms",
                "Competitive pricing",
                "Excellent customer reviews"
            ],
            weaknesses: [
                "Limited availability during peak season",
                "No discounts for long stays"
            ],
            recommendations: [
                {
                    title: "Price Strategy",
                    description: "Consider matching Agoda's pricing to gain market share"
                },
                {
                    title: "Marketing",
                    description: "Promote high ratings on official website"
                }
            ]
        }
    };
    
    displayResults(mockData);
    showResults(true);
}
*/
