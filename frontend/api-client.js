// hotel-scraper/frontend/api-client.js
// Simple API client examples for hotel scraper

/**
 * Hotel Scraper API Client
 * Usage examples for calling the Spring Boot REST API
 */

// Configuration
const API_BASE_URL = 'http://localhost:9090/api';  // Primary backend on port 9090
const TIMEOUT_MS = 30000;

// Fallbacks in case API is reachable at other common ports:
// const API_BASE_URL = 'http://localhost:8081/api';
// const API_BASE_URL = 'http://localhost:8080/api';

/**
 * Example 1: Basic API call with fetch
 */
async function exampleBasicFetch() {
    try {
        const hotelName = 'Taj Hotel Mumbai';
        const response = await fetch(
            `${API_BASE_URL}/scrape?hotelName=${encodeURIComponent(hotelName)}`
        );
        
        if (!response.ok) {
            throw new Error(`API Error: ${response.status}`);
        }
        
        const data = await response.json();
        console.log('Hotel Data:', data);
        
    } catch (error) {
        console.error('Error:', error);
    }
}

/**
 * Example 2: API call with error handling
 */
async function exampleWithErrorHandling() {
    const hotelName = 'Taj Hotel';
    
    try {
        const response = await fetch(
            `${API_BASE_URL}/scrape?hotelName=${encodeURIComponent(hotelName)}`,
            {
                method: 'GET',
                headers: {
                    'Content-Type': 'application/json',
                },
                timeout: TIMEOUT_MS
            }
        );
        
        const data = await response.json();
        
        if (data.error) {
            console.error('API returned error:', data.error);
            return null;
        }
        
        console.log('Success:', data.hotel);
        return data;
        
    } catch (error) {
        console.error('Network error:', error.message);
        return null;
    }
}

/**
 * Example 3: Using Fetch with AbortController (for timeout)
 */
async function exampleWithTimeout() {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), TIMEOUT_MS);
    
    try {
        const response = await fetch(
            `${API_BASE_URL}/scrape?hotelName=Taj%20Hotel`,
            {
                signal: controller.signal
            }
        );
        
        clearTimeout(timeoutId);
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }
        
        const data = await response.json();
        console.log('Data:', data);
        
    } catch (error) {
        if (error.name === 'AbortError') {
            console.error('Request timeout');
        } else {
            console.error('Error:', error);
        }
    }
}

/**
 * Example 4: Using XMLHttpRequest (older method)
 */
function exampleWithXHR() {
    const xhr = new XMLHttpRequest();
    const hotelName = 'Taj Hotel Mumbai';
    
    xhr.onload = function() {
        if (xhr.status >= 200 && xhr.status < 300) {
            const data = JSON.parse(xhr.responseText);
            console.log('Hotel:', data.hotel.name);
        } else {
            console.error('Error:', xhr.statusText);
        }
    };
    
    xhr.onerror = function() {
        console.error('Network error');
    };
    
    xhr.open(
        'GET',
        `${API_BASE_URL}/scrape?hotelName=${encodeURIComponent(hotelName)}`
    );
    xhr.send();
}

/**
 * Example 5: Using Axios (if available)
 */
async function exampleWithAxios() {
    // Note: Add <script src="https://cdn.jsdelivr.net/npm/axios/dist/axios.min.js"></script> first
    
    try {
        const response = await axios.get(
            `${API_BASE_URL}/scrape`,
            {
                params: { hotelName: 'Taj Hotel Mumbai' },
                timeout: TIMEOUT_MS
            }
        );
        
        console.log('Data:', response.data);
        
    } catch (error) {
        console.error('Error:', error.message);
    }
}

/**
 * Example 6: Check API Health
 */
async function checkAPIHealth() {
    try {
        const response = await fetch(`${API_BASE_URL}/health`);
        const data = await response.json();
        
        if (response.ok) {
            console.log('✅ API is healthy:', data.status);
            return true;
        } else {
            console.log('❌ API is down');
            return false;
        }
    } catch (error) {
        console.log('❌ Cannot reach API:', error.message);
        return false;
    }
}

/**
 * Example 7: Display formatted hotel info
 */
async function displayHotelInfo(hotelName) {
    try {
        const response = await fetch(
            `${API_BASE_URL}/scrape?hotelName=${encodeURIComponent(hotelName)}`
        );
        
        const data = await response.json();
        
        if (data.error) {
            console.error('❌', data.error);
            return;
        }
        
        const hotel = data.hotel;
        
        console.log('\n' + '='.repeat(50));
        console.log(`🏨 ${hotel.name}`);
        console.log('='.repeat(50));
        console.log(`📍 ${hotel.address}`);
        console.log(`💰 ${hotel.price}`);
        console.log(`⭐ ${hotel.rating} (${hotel.reviews} reviews)`);
        
        if (hotel.amenities && hotel.amenities.length > 0) {
            console.log(`🛎️  Amenities: ${hotel.amenities.slice(0, 5).join(', ')}`);
        }
        
        if (data.rooms && data.rooms.length > 0) {
            console.log(`\n🛏️  Available Rooms:`);
            data.rooms.forEach((room, i) => {
                console.log(`  ${i + 1}. ${room.roomType} - ${room.price} (${room.guests} guests)`);
            });
        }
        
        console.log('\n' + '='.repeat(50));
        
    } catch (error) {
        console.error('Error:', error);
    }
}

/**
 * Example 8: Compare Multiple Hotels
 */
async function compareHotels(hotelNames) {
    const results = [];
    
    for (const hotelName of hotelNames) {
        try {
            const response = await fetch(
                `${API_BASE_URL}/scrape?hotelName=${encodeURIComponent(hotelName)}`
            );
            
            const data = await response.json();
            
            if (!data.error && data.hotel) {
                results.push({
                    name: data.hotel.name,
                    price: data.hotel.price,
                    rating: data.hotel.rating
                });
            }
        } catch (error) {
            console.error(`Error scraping ${hotelName}:`, error);
        }
    }
    
    // Sort by price
    results.sort((a, b) => parseInt(a.price) - parseInt(b.price));
    
    console.log('\nHotel Price Comparison:');
    results.forEach((hotel, i) => {
        console.log(`${i + 1}. ${hotel.name} - ${hotel.price} (Rating: ${hotel.rating})`);
    });
    
    return results;
}

/**
 * Example 9: Batch scraping with delays
 */
async function batchScrapeWithDelay(hotelNames, delayMs = 2000) {
    const results = [];
    
    for (const hotelName of hotelNames) {
        try {
            console.log(`Scraping: ${hotelName}...`);
            
            const response = await fetch(
                `${API_BASE_URL}/scrape?hotelName=${encodeURIComponent(hotelName)}`
            );
            
            const data = await response.json();
            
            if (!data.error) {
                results.push(data);
            }
            
            // Wait before next request
            await new Promise(resolve => setTimeout(resolve, delayMs));
            
        } catch (error) {
            console.error(`Error: ${hotelName}`, error);
        }
    }
    
    return results;
}

/**
 * Example 10: Full integration example
 */
async function integratedExample() {
    console.log('🏨 Hotel Scraper API - Integrated Example\n');
    
    // Step 1: Check health
    console.log('Step 1: Checking API health...');
    const isHealthy = await checkAPIHealth();
    
    if (!isHealthy) {
        console.log('Cannot proceed - API is not running');
        console.log('Start it with: run-api.bat (or run-api.sh)');
        return;
    }
    
    // Step 2: Scrape single hotel
    console.log('\nStep 2: Scraping single hotel...');
    await displayHotelInfo('Taj Hotel Mumbai');
    
    // Step 3: Compare multiple hotels
    console.log('\nStep 3: Comparing hotels...');
    const hotels = ['Taj Hotel', 'Four Seasons', 'ITC Hotels'];
    await compareHotels(hotels);
    
    console.log('\n✅ Example complete!');
}

// ============================================
// USAGE IN HTML
// ============================================

/*

<!-- Option 1: Add to HTML button -->
<button onclick="exampleBasicFetch()">Test API</button>

<!-- Option 2: Add to HTML and run on page load -->
<script>
    window.addEventListener('load', async () => {
        await integratedExample();
    });
</script>

<!-- Option 3: Open browser console and run -->
// Paste any example function above, then call it:
// displayHotelInfo('Taj Hotel');

*/

// Export for module usage
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        exampleBasicFetch,
        exampleWithErrorHandling,
        checkAPIHealth,
        displayHotelInfo,
        compareHotels,
        batchScrapeWithDelay,
        integratedExample
    };
}
