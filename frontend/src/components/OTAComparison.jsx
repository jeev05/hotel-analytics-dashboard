function fmt(n) {
  if (n == null || Number.isNaN(n)) {
    return '—';
  }
  if (typeof n === 'number') {
    return String(n);
  }
  return String(n);
}

function formatReviewLabel(value) {
  if (value == null || Number.isNaN(value)) {
    return '—';
  }
  const num = Number(String(value).replace(/[^0-9]/g, ''));
  if (Number.isNaN(num)) {
    return String(value);
  }
  return `${num.toLocaleString('en-US')} reviews`;
}

function OTAComparison({ hotel, competitors }) {
  // Try to source values from hotel object first, then fall back to competitors list
  const booking = (hotel && hotel.booking) || (Array.isArray(competitors) ? competitors.find((c) => String(c.source || '').toLowerCase().includes('booking')) : null);
  const agoda = (hotel && hotel.agoda) || (Array.isArray(competitors) ? competitors.find((c) => String(c.source || '').toLowerCase().includes('agoda')) : null);

  const normalizeSource = (sourceData) => {
    if (!sourceData) {
      return null;
    }
    return {
      price: sourceData.price || sourceData.avgPrice || null,
      rating: sourceData.rating || null,
      reviewCount: sourceData.reviewCount ?? sourceData.reviews ?? null,
      amenitiesCount: sourceData.amenities?.length ?? sourceData.amenitiesCount ?? null,
      policiesCount: sourceData.policies?.length ?? sourceData.policiesCount ?? null,
      roomTypesCount: sourceData.roomTypesCount ?? sourceData.roomCount ?? sourceData.rooms?.length ?? null,
      imageScore: sourceData.imageScore ?? null
    };
  };

  const bookingSource = normalizeSource(booking);
  const agodaSource = normalizeSource(agoda);

  const hasValidSource = (source) => {
    if (!source) return false;
    return [source.price, source.rating, source.reviewCount, source.amenitiesCount, source.policiesCount, source.roomTypesCount, source.imageScore].some((value) => value != null && value !== 'N/A' && value !== '—');
  };

  if (!hasValidSource(bookingSource) || !hasValidSource(agodaSource)) {
    return (
      <section>
        <p className="text-sm uppercase tracking-[0.3em] text-slate-500 dark:text-slate-400">Booking.com vs Agoda</p>
        <h2 className="mt-2 text-2xl font-semibold text-slate-900 dark:text-slate-100">Booking.com vs Agoda Comparison</h2>
        <div className="mt-4 rounded-3xl bg-slate-50 p-5 text-slate-700 dark:bg-slate-900/80 dark:text-slate-300">
          Search the same hotel on Booking.com and Agoda to enable comparison.
        </div>
      </section>
    );
  }

  const rows = [
    { key: 'price', label: 'Price', booking: bookingSource?.price ?? null, agoda: agodaSource?.price ?? null },
    { key: 'rating', label: 'Rating', booking: bookingSource?.rating ?? null, agoda: agodaSource?.rating ?? null },
    { key: 'reviewCount', label: 'Review Count', booking: bookingSource?.reviewCount ?? null, agoda: agodaSource?.reviewCount ?? null, formatter: formatReviewLabel },
    { key: 'amenities', label: 'Amenities Count', booking: bookingSource?.amenitiesCount ?? null, agoda: agodaSource?.amenitiesCount ?? null }
  ];

  return (
    <section>
      <p className="text-sm uppercase tracking-[0.3em] text-slate-500 dark:text-slate-400">Booking.com vs Agoda</p>
      <h2 className="mt-2 text-2xl font-semibold text-slate-900 dark:text-slate-100">Booking.com vs Agoda Comparison</h2>
      <div className="mt-4 overflow-hidden rounded-2xl border border-slate-200 dark:border-slate-700">
        <table className="min-w-full divide-y divide-slate-200 dark:divide-slate-700 text-sm text-slate-700 dark:text-slate-200">
          <thead className="bg-slate-50 text-left uppercase tracking-[0.3em] text-slate-500 dark:bg-slate-900 dark:text-slate-400">
            <tr>
              <th className="px-4 py-3">Metric</th>
              <th className="px-4 py-3">Booking.com</th>
              <th className="px-4 py-3">Agoda</th>
              <th className="px-4 py-3">Difference</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-200 bg-white dark:divide-slate-700 dark:bg-slate-950">
            {rows.map((r) => {
              const b = typeof r.booking === 'string' ? r.booking : (r.booking == null ? null : r.booking);
              const a = typeof r.agoda === 'string' ? r.agoda : (r.agoda == null ? null : r.agoda);
              let diff = '—';
              const bn = Number(String(b).replace(/[^0-9.\-]/g, ''));
              const an = Number(String(a).replace(/[^0-9.\-]/g, ''));
              if (!Number.isNaN(bn) && !Number.isNaN(an)) {
                if (r.key === 'price') {
                  diff = `${Math.round(((bn - an) / Math.max(1, an)) * 100)}%`;
                } else {
                  const d = bn - an;
                  diff = String(d);
                }
              }

              return (
                <tr key={r.key} className="even:bg-slate-50 odd:bg-white dark:even:bg-slate-900 dark:odd:bg-slate-950">
                  <td className="px-4 py-4">{r.label}</td>
                  <td className="px-4 py-4 whitespace-nowrap">{fmt(b)}</td>
                  <td className="px-4 py-4 whitespace-nowrap">{fmt(a)}</td>
                  <td className="px-4 py-4 whitespace-nowrap">{diff}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
      {/* Extra comparison details when backend provides a comparison object */}
      {hotel?.comparison ? (
        <div className="mt-4 rounded-2xl bg-slate-50 p-5 text-slate-700 dark:bg-slate-900/80 dark:text-slate-300">
          <h3 className="text-lg font-semibold">Comparison Summary</h3>
          <div className="mt-2 grid gap-2">
            <div>Rating Winner: <strong>{hotel.comparison.ratingWinner || '—'}</strong></div>
            <div>Amenities Winner: <strong>{hotel.comparison.amenitiesWinner || '—'}</strong></div>
            <div>Booking Amenities: <strong>{hotel.comparison.bookingAmenitiesCount ?? '—'}</strong></div>
            <div>Agoda Amenities: <strong>{hotel.comparison.agodaAmenitiesCount ?? '—'}</strong></div>
            <div className="pt-2">
              <strong>Common Amenities:</strong>
              <div className="mt-1 text-slate-600 dark:text-slate-400 break-words">{(hotel.comparison.commonAmenities || []).join(', ') || '—'}</div>
            </div>
            <div className="pt-2">
              <strong>Booking Only:</strong>
              <div className="mt-1 text-slate-600 dark:text-slate-400 break-words">{(hotel.comparison.bookingOnlyAmenities || []).join(', ') || '—'}</div>
            </div>
            <div className="pt-2">
              <strong>Agoda Only:</strong>
              <div className="mt-1 text-slate-600 dark:text-slate-400 break-words">{(hotel.comparison.agodaOnlyAmenities || []).join(', ') || '—'}</div>
            </div>
            {hotel.comparison.improvementSuggestions && hotel.comparison.improvementSuggestions.length > 0 ? (
              <div className="pt-2">
                <strong>Improvement Suggestions:</strong>
                <ul className="list-disc pl-6 mt-1">{hotel.comparison.improvementSuggestions.map((s) => <li key={s}>{s}</li>)}</ul>
              </div>
            ) : null}
            {hotel.comparison.overallRecommendation ? (
              <div className="pt-3">
                <strong>Overall Recommendation:</strong>
                <p className="mt-1">{hotel.comparison.overallRecommendation}</p>
              </div>
            ) : null}
          </div>
        </div>
      ) : null}
    </section>
  );
}

export default OTAComparison;
