import axios from 'axios';

const apiClient = axios.create({
  baseURL: '/api',
  timeout: 300000,
  headers: {
    'Content-Type': 'application/json'
  }
});

export async function fetchHotelAnalytics(hotelName, source) {
  const response = await apiClient.get('/scrape', {
    params: {
      hotelName,
      source
    }
  });

  // Log raw API response for debugging rendering issues
  console.log('API Response:', response.data);

  if (response.data?.error) {
    throw new Error(response.data.error);
  }

  return response.data;
}

export async function checkBackendHealth() {
  const response = await apiClient.get('/health');
  return response.data;
}
