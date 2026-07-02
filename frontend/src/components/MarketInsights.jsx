function parseNumber(v) {
  if (v == null) return NaN;
  const n = Number(String(v).replace(/[^0-9.\-\.]/g, ''));
  return Number.isFinite(n) ? n : NaN;
}

function avg(arr) {
  const nums = arr.map(parseNumber).filter((n) => !Number.isNaN(n));
  if (!nums.length) return NaN;
  return nums.reduce((s, v) => s + v, 0) / nums.length;
}

function MarketInsights({ hotel, competitors }) {
  const compList = Array.isArray(competitors) ? competitors : [];
  const hasCompetitorData = compList.some((c) => c && (c.price || c.rating || c.reviewCount || c.reviews));

  // Price analysis
  const hotelPrice = parseNumber(hotel?.price);
  const compAvgPrice = avg(compList.map((c) => c.price || c.avgPrice));
  const pricePosition = (!Number.isNaN(hotelPrice) && !Number.isNaN(compAvgPrice)) ? `${Math.round(((hotelPrice - compAvgPrice) / Math.max(1, compAvgPrice)) * 100)}%` : '—';

  // Review analysis
  const hotelRating = parseNumber(hotel?.rating);
  const compAvgRating = avg(compList.map((c) => c.rating));
  const hotelReviews = parseNumber(hotel?.reviews);
  const compAvgReviews = avg(compList.map((c) => c.reviewCount || c.reviews));

  // Amenities gap
  const hotelAmenities = Array.isArray(hotel?.amenities) ? hotel.amenities : [];
  const compAmenCounts = compList.map((c) => (Array.isArray(c.amenities) ? c.amenities.length : parseNumber(c.amenitiesCount) || 0));
  const compAvgAmenities = Math.round(avg(compAmenCounts) || 0);
  const amenityGap = hotelAmenities.length != null ? Math.max(0, compAvgAmenities - hotelAmenities.length) : '—';

  const competitorsHave = hasCompetitorData
    ? Array.from(new Set(compList.flatMap((c) => Array.isArray(c.amenities) ? c.amenities : []))).slice(0, 20)
    : [];
  const missingAmenities = hasCompetitorData
    ? competitorsHave.filter((a) => !hotelAmenities.includes(a))
    : [];

  // Market ranking (based on price or rating density)
  let marketRank = '—';
  if (hotel && hasCompetitorData) {
    const sorted = [...compList].sort((a, b) => (parseNumber(b.rating) || 0) - (parseNumber(a.rating) || 0));
    const names = sorted.map((c) => c.name || '');
    const idx = names.indexOf(hotel?.name);
    if (idx >= 0) marketRank = `#${idx + 1} of ${sorted.length}`;
  }

  // Top improvement opportunities generated only from real measured deltas
  const opportunities = [];
  if (hasCompetitorData && !Number.isNaN(hotelRating) && !Number.isNaN(compAvgRating) && hotelRating < compAvgRating) {
    const diff = (compAvgRating - hotelRating).toFixed(2);
    opportunities.push(`Review score is ${diff} below market average.`);
  }
  if (hasCompetitorData && Array.isArray(missingAmenities) && missingAmenities.length) {
    const topMissing = missingAmenities.slice(0, 3);
    topMissing.forEach((m) => opportunities.push(`Missing amenity: ${m}`));
  }
  if (hasCompetitorData && !Number.isNaN(hotelPrice) && !Number.isNaN(compAvgPrice)) {
    const percent = Math.round(((hotelPrice - compAvgPrice) / Math.max(1, compAvgPrice)) * 100);
    if (Math.abs(percent) >= 5) {
      opportunities.push(`Price is ${percent > 0 ? `${percent}% above` : `${Math.abs(percent)}% below`} market average.`);
    }
  }

  return (
    <section>
      <p className="text-sm uppercase tracking-[0.3em] text-slate-500 dark:text-slate-400">Market insights</p>
      <h2 className="mt-2 text-2xl font-semibold text-slate-900 dark:text-slate-100">Market Insights</h2>

      <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-2">
        <div className="rounded-3xl bg-slate-50 p-5 dark:bg-slate-900/80">
          <p className="text-sm uppercase tracking-[0.3em] text-slate-500 dark:text-slate-400">Price Position</p>
          <p className="mt-3 text-lg font-semibold text-slate-900 dark:text-slate-100">Your Hotel: {hotelPrice || '—'}</p>
          <p className="text-sm text-slate-600 dark:text-slate-400">Competitor Average: {Number.isNaN(compAvgPrice) ? '—' : Math.round(compAvgPrice)}</p>
          <p className="mt-2 text-sm text-slate-700 dark:text-slate-300">Difference: {pricePosition}</p>
        </div>

        <div className="rounded-3xl bg-slate-50 p-5 dark:bg-slate-900/80">
          <p className="text-sm uppercase tracking-[0.3em] text-slate-500 dark:text-slate-400">Review Position</p>
          <p className="mt-3 text-lg font-semibold text-slate-900 dark:text-slate-100">Your Rating: {hotelRating || '—'}</p>
          <p className="text-sm text-slate-600 dark:text-slate-400">Competitor Average: {Number.isNaN(compAvgRating) ? '—' : compAvgRating.toFixed(2)}</p>
          <p className="mt-2 text-sm text-slate-700 dark:text-slate-300">Difference: {(!Number.isNaN(hotelRating) && !Number.isNaN(compAvgRating)) ? (hotelRating - compAvgRating).toFixed(2) : '—'}</p>
        </div>
      </div>

      <div className="mt-6 grid gap-4 sm:grid-cols-2">
        <div className="rounded-3xl bg-slate-50 p-5 dark:bg-slate-900/80">
          <p className="text-sm uppercase tracking-[0.3em] text-slate-500 dark:text-slate-400">Review Volume</p>
          <p className="mt-3 text-lg font-semibold text-slate-900 dark:text-slate-100">Your Reviews: {hotelReviews || '—'}</p>
          <p className="text-sm text-slate-600 dark:text-slate-400">Competitor Average: {Number.isNaN(compAvgReviews) ? '—' : Math.round(compAvgReviews)}</p>
          <p className="mt-2 text-sm text-slate-700 dark:text-slate-300">Difference: {(!Number.isNaN(hotelReviews) && !Number.isNaN(compAvgReviews)) ? (hotelReviews - compAvgReviews) : '—'}</p>
        </div>

        <div className="rounded-3xl bg-slate-50 p-5 dark:bg-slate-900/80">
          <p className="text-sm uppercase tracking-[0.3em] text-slate-500 dark:text-slate-400">Market Ranking</p>
          <p className="mt-3 text-lg font-semibold text-slate-900 dark:text-slate-100">{marketRank}</p>
        </div>
      </div>

      <div className="mt-6">
        <div className="rounded-3xl bg-slate-50 p-5 dark:bg-slate-900/80">
          <p className="text-sm uppercase tracking-[0.3em] text-slate-500 dark:text-slate-400">Amenity gap analysis</p>
          {hasCompetitorData ? (
            <>
              <p className="mt-3 text-sm text-slate-700 dark:text-slate-300">Competitors have:</p>
              <ul className="mt-2 grid gap-2 sm:grid-cols-2">
                {competitorsHave.slice(0, 8).map((am, i) => (
                  <li key={am + i} className="inline-flex items-center gap-2 rounded-full bg-white px-3 py-2 text-sm font-medium text-slate-900 shadow-sm dark:bg-slate-950/80 dark:text-slate-100">✓ {am}</li>
                ))}
              </ul>

              <p className="mt-4 text-sm text-slate-700 dark:text-slate-300">Your hotel missing:</p>
              {missingAmenities.length ? (
                <ul className="mt-2 space-y-2 text-sm text-slate-700 dark:text-slate-300">
                  {missingAmenities.slice(0, 6).map((m, i) => (
                    <li key={m + i} className="rounded-3xl bg-white p-3 shadow-sm dark:bg-slate-950/80">✗ {m}</li>
                  ))}
                </ul>
              ) : (
                <p className="mt-2 text-sm text-slate-600 dark:text-slate-400">No structured missing amenities detected.</p>
              )}

              <p className="mt-4 text-sm text-slate-700 dark:text-slate-300">Your Amenities: {hotelAmenities.length}</p>
              <p className="text-sm text-slate-700 dark:text-slate-300">Competitor Average: {compAvgAmenities}</p>
              <p className="text-sm text-slate-700 dark:text-slate-300">Gap: {typeof amenityGap === 'number' ? amenityGap : '—'}</p>
            </>
          ) : (
            <p className="mt-3 text-sm text-slate-600 dark:text-slate-400">Competitor amenity data is not available for this search.</p>
          )}
        </div>
      </div>

      <div className="mt-6">
        <div className="rounded-3xl bg-slate-50 p-5 dark:bg-slate-900/80">
          <p className="text-sm uppercase tracking-[0.3em] text-slate-500 dark:text-slate-400">Top improvement opportunities</p>
          {hasCompetitorData ? (
            opportunities.length ? (
              <ol className="mt-3 list-decimal space-y-2 pl-5 text-sm text-slate-700 dark:text-slate-300">
                {opportunities.map((o, i) => (
                  <li key={i} className="rounded-3xl bg-white p-3 shadow-sm dark:bg-slate-950/80">{o}</li>
                ))}
              </ol>
            ) : (
              <p className="mt-3 text-sm text-slate-600 dark:text-slate-400">No immediate improvement opportunities detected from structured competitor data.</p>
            )
          ) : (
            <p className="mt-3 text-sm text-slate-600 dark:text-slate-400">Not enough competitor data available to generate opportunities.</p>
          )}
        </div>
      </div>
    </section>
  );
}

export default MarketInsights;
