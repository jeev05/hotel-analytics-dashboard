import { useEffect, useState } from 'react';
import { fetchHotelAnalytics, checkBackendHealth } from '../api';
import { normalizeHotelData, attachComparison } from '../dataNormalization';
import HotelCard from './HotelCard';
import StatsCards from './StatsCards';
import CompetitorAnalysis from './CompetitorAnalysis';
import RoomTable from './RoomTable';
import Amenities from './Amenities';
import OTAComparison from './OTAComparison';
import MarketInsights from './MarketInsights';

const SOURCE_OPTIONS = [
  { value: 'booking', label: 'Booking.com' },
  { value: 'agoda', label: 'Agoda' }
];

function Dashboard() {
  const [hotelName, setHotelName] = useState('');
  const [source, setSource] = useState('booking');
  const [hotelData, setHotelData] = useState(null);
  const [rooms, setRooms] = useState([]);
  const [analysis, setAnalysis] = useState([]);
  const [statusMessage, setStatusMessage] = useState('Checking backend connection...');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [theme, setTheme] = useState('light');

  useEffect(() => {
    document.documentElement.classList.toggle('dark', theme === 'dark');
  }, [theme]);

  useEffect(() => {
    async function initHealth() {
      try {
        await checkBackendHealth();
        setStatusMessage('Backend connected — analytics ready.');
      } catch (err) {
        setStatusMessage('Backend unreachable. Start Spring Boot on port 9090.');
      }
    }

    initHealth();
  }, []);

  const handleSearch = async () => {
    if (!hotelName.trim()) {
      setError('Please enter a hotel name to search.');
      return;
    }

    setError('');
    setLoading(true);
    setHotelData(null);
    setRooms([]);
    setAnalysis([]);

    try {
      const response = await fetchHotelAnalytics(hotelName.trim(), source);
      let normalized = normalizeHotelData(response);
      normalized = attachComparison(normalized, response);
      setHotelData(normalized.hotel);
      setRooms(normalized.rooms);
      setAnalysis(normalized.competitors);
      if (response.error) {
        setError(response.error);
      }
    } catch (err) {
      setError(err.message || 'Unable to fetch hotel data.');
    } finally {
      setLoading(false);
    }
  };

  const handleKeyPress = (event) => {
    if (event.key === 'Enter') {
      handleSearch();
    }
  };

  // listing consistency calculation — combines price, rating, amenities, rooms, policies
  function computeListingConsistency(hotel, competitors) {
    if (!hotel || !Array.isArray(competitors) || !competitors.length) return null;

    const hotelPrice = parseNumeric(hotel?.price);
    const avgPrice = getAverageCompetitorPrice(competitors);
    const priceScore = Number.isNaN(hotelPrice) || Number.isNaN(avgPrice) ? 100 : Math.max(0, 100 - (Math.abs(hotelPrice - avgPrice) / Math.max(1, avgPrice)) * 100);

    const compRatings = competitors.map((c) => parseFloat(c.rating)).filter((v) => !Number.isNaN(v));
    const avgRating = compRatings.length ? compRatings.reduce((s, v) => s + v, 0) / compRatings.length : NaN;
    const ratingScore = Number.isNaN(hotel?.rating) || Number.isNaN(avgRating) ? 100 : Math.max(0, 100 - (Math.abs(Number(hotel.rating) - avgRating) / Math.max(0.1, avgRating)) * 100);

    const hotelAmenities = (hotel?.amenities || []).length;
    const compAmenities = competitors.map((c) => (Array.isArray(c.amenities) ? c.amenities.length : parseInt(c.amenitiesCount || '0', 10) || 0));
    const avgAmenities = compAmenities.length ? compAmenities.reduce((s, v) => s + v, 0) / compAmenities.length : NaN;
    const amenitiesScore = Number.isNaN(avgAmenities) ? 100 : Math.max(0, 100 - (Math.abs(hotelAmenities - avgAmenities) / Math.max(1, avgAmenities)) * 100);

    const hotelRooms = hotel?.roomCount || 0;
    const compRooms = competitors.map((c) => c.roomTypesCount || 0);
    const avgRooms = compRooms.length ? compRooms.reduce((s, v) => s + v, 0) / compRooms.length : NaN;
    const roomsScore = Number.isNaN(avgRooms) ? 100 : Math.max(0, 100 - (Math.abs(hotelRooms - avgRooms) / Math.max(1, avgRooms)) * 100);

    const hotelPolicies = (hotel?.policies || []).length;
    const compPolicies = competitors.map((c) => (Array.isArray(c.policies) ? c.policies.length : parseInt(c.policiesCount || '0', 10) || 0));
    const avgPolicies = compPolicies.length ? compPolicies.reduce((s, v) => s + v, 0) / compPolicies.length : NaN;
    const policiesScore = Number.isNaN(avgPolicies) ? 100 : Math.max(0, 100 - (Math.abs(hotelPolicies - avgPolicies) / Math.max(1, avgPolicies)) * 100);

    const components = [priceScore, ratingScore, amenitiesScore, roomsScore, policiesScore].map((n) => Number.isFinite(n) ? n : 100);
    const score = Math.round(components.reduce((s, v) => s + v, 0) / components.length);
    return score;
  }

  const listingConsistency = computeListingConsistency(hotelData, analysis);

  return (
    <div className="mx-auto min-h-screen max-w-[1400px] overflow-x-hidden px-4 py-6 sm:px-6 lg:px-8">
      <header className="mb-8 rounded-3xl bg-slate-900 px-6 py-6 text-white shadow-lg shadow-slate-900/10">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <p className="text-sm uppercase tracking-[0.3em] text-slate-400">Hotel analytics</p>
            <h1 className="mt-2 text-3xl font-semibold tracking-tight sm:text-4xl">Hotel performance dashboard</h1>
          </div>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-1">
            <button
              type="button"
              onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}
              className="rounded-full bg-slate-800 px-4 py-2 text-sm font-semibold text-slate-100 transition hover:bg-slate-700"
            >
              {theme === 'dark' ? 'Light mode' : 'Dark mode'}
            </button>
            <div className="rounded-3xl bg-slate-800 px-4 py-3 text-sm text-slate-300">
              {statusMessage}
            </div>
          </div>
        </div>
      </header>

      <section className="mb-6 grid gap-6 lg:grid-cols-[1.6fr_0.9fr]">
        <div className="rounded-3xl border border-slate-200/80 bg-white/95 p-6 shadow-sm dark:border-slate-700/80 dark:bg-slate-950/90">
          <div className="grid gap-4 sm:grid-cols-[1.8fr_1fr]">
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700 dark:text-slate-300">Hotel name</span>
              <input
                type="text"
                value={hotelName}
                onChange={(event) => setHotelName(event.target.value)}
                onKeyDown={handleKeyPress}
                placeholder="Enter hotel name, e.g. Taj Mahal Palace Mumbai"
                className="w-full rounded-3xl border border-slate-300 bg-slate-50 px-4 py-3 text-sm text-slate-900 shadow-sm outline-none transition focus:border-sky-500 focus:ring-2 focus:ring-sky-100 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100 dark:focus:border-cyan-400 dark:focus:ring-cyan-900/30"
              />
            </label>
            <label className="block">
              <span className="mb-2 block text-sm font-medium text-slate-700 dark:text-slate-300">Source</span>
              <select
                value={source}
                onChange={(event) => setSource(event.target.value)}
                className="w-full rounded-3xl border border-slate-300 bg-slate-50 px-4 py-3 text-sm text-slate-900 shadow-sm outline-none transition focus:border-sky-500 focus:ring-2 focus:ring-sky-100 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-100 dark:focus:border-cyan-400 dark:focus:ring-cyan-900/30"
              >
                {SOURCE_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <div className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
            <button
              onClick={handleSearch}
              disabled={loading}
              className="inline-flex items-center justify-center rounded-3xl bg-slate-900 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-500"
            >
              {loading ? 'Analyzing hotel...' : 'Run analysis'}
            </button>
            {error ? (
              <div className="rounded-3xl bg-rose-50 px-4 py-3 text-sm text-rose-700 ring-1 ring-rose-200 dark:bg-rose-950/30 dark:text-rose-200">
                {error}
              </div>
            ) : null}
          </div>
          {loading && source === 'agoda' ? (
            <div className="mt-4 rounded-3xl bg-slate-100 px-4 py-3 text-sm text-slate-700 ring-1 ring-slate-200 dark:bg-slate-900/80 dark:text-slate-200 dark:ring-slate-700/80">
              <p className="font-semibold">Analyzing Agoda listing.</p>
              <p>This may take up to 3–5 minutes. Please keep this page open while the backend completes the scrape.</p>
            </div>
          ) : null}
        </div>

        <div className="grid gap-6">
          <StatsCards hotel={hotelData} consistency={listingConsistency} />
        </div>
      </section>

      <main className="grid gap-6 xl:grid-cols-[1.55fr_1fr]">
        <div className="space-y-6">
          <HotelCard hotel={hotelData} source={source} />
          <div className="grid gap-4 lg:grid-cols-2">
            <div className="rounded-3xl border border-slate-200/80 bg-white/95 p-6 shadow-sm dark:border-slate-700/80 dark:bg-slate-950/90">
              <CompetitorAnalysis analysis={analysis} />
            </div>
            <div className="rounded-3xl border border-slate-200/80 bg-white/95 p-6 shadow-sm dark:border-slate-700/80 dark:bg-slate-950/90">
              <MarketInsights hotel={hotelData} competitors={analysis} />
            </div>
          </div>

          {hotelData ? (
            <div className="rounded-3xl border border-slate-200/80 bg-white/95 p-6 shadow-sm dark:border-slate-700/80 dark:bg-slate-950/90">
              <OTAComparison hotel={hotelData} competitors={analysis} />
            </div>
          ) : null}
          <Amenities hotel={hotelData} />
          <RoomTable rooms={rooms} />
        </div>

        <div className="space-y-6">
          <div className="rounded-3xl border border-slate-200/80 bg-white/95 p-6 shadow-sm dark:border-slate-700/80 dark:bg-slate-950/90">
            <p className="text-sm uppercase tracking-[0.3em] text-slate-500 dark:text-slate-400">Image analysis</p>
            <h2 className="mt-2 text-2xl font-semibold text-slate-900 dark:text-slate-100">Listing media quality</h2>
            <div className="mt-6 grid gap-4 sm:grid-cols-2">
              <div className="rounded-3xl bg-slate-50 p-5 dark:bg-slate-900/80">
                <p className="text-sm uppercase tracking-[0.3em] text-slate-500 dark:text-slate-400">Image score</p>
                <p className="mt-3 text-4xl font-semibold text-slate-900 dark:text-slate-100">{typeof hotelData?.imageScore === 'number' ? `${Math.round(hotelData.imageScore)} / 100` : '—'}</p>
              </div>
              <div className="rounded-3xl bg-slate-50 p-5 dark:bg-slate-900/80">
                <p className="text-sm uppercase tracking-[0.3em] text-slate-500 dark:text-slate-400">Status</p>
                <p className="mt-3 text-3xl font-semibold text-slate-900 dark:text-slate-100">{hotelData ? (hotelData.blurry ? 'Review' : 'Good') : '—'}</p>
              </div>
            </div>
            {/* Generic issues/recommendations removed — image analysis shows only measured metrics */}
          </div>
        </div>
      </main>
    </div>
  );
}

function parseNumeric(text) {
  if (!text) {
    return NaN;
  }
  const match = String(text).replace(/,/g, '').match(/([0-9]+(?:\.[0-9]+)?)/);
  return match ? Number(match[1]) : NaN;
}

function getAverageCompetitorPrice(analysis) {
  if (!Array.isArray(analysis) || !analysis.length) {
    return NaN;
  }

  const prices = analysis
    .map((item) => parseNumeric(item.price))
    .filter((value) => !Number.isNaN(value));

  if (!prices.length) {
    return NaN;
  }

  return prices.reduce((sum, value) => sum + value, 0) / prices.length;
}



export default Dashboard;
