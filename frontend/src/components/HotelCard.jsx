function HotelCard({ hotel, source }) {
  const sourceLabel = source === 'agoda' ? 'Agoda' : 'Booking.com';

  const formatReviewLabel = (value) => {
    if (value == null || Number.isNaN(Number(String(value).replace(/[^0-9]/g, '')))) {
      return '—';
    }
    const numeric = Number(String(value).replace(/[^0-9]/g, ''));
    return `${numeric.toLocaleString('en-US')} reviews`;
  };

  if (!hotel) {
    return (
      <div className="rounded-3xl border border-dashed border-slate-300/70 bg-white/95 p-8 text-center text-slate-500 shadow-sm dark:border-slate-700/70 dark:bg-slate-950/90 dark:text-slate-400">
        <p className="text-lg font-semibold">Search a hotel to populate the dashboard.</p>
        <p className="mt-2 text-sm">The summary card will display the selected hotel and source.</p>
      </div>
    );
  }

  return (
    <section className="rounded-3xl border border-slate-200/80 bg-white/95 p-6 shadow-sm dark:border-slate-700/80 dark:bg-slate-950/90">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <p className="text-sm uppercase tracking-[0.3em] text-slate-500 dark:text-slate-400">Hotel summary</p>
          <h2 className="mt-2 text-3xl font-semibold text-slate-900 dark:text-slate-100">{hotel.name}</h2>
          <p className="mt-3 text-sm text-slate-600 dark:text-slate-400">{hotel.address || 'Address unavailable'}</p>
        </div>
        <span className="rounded-full bg-slate-100 px-4 py-2 text-sm font-semibold text-slate-700 dark:bg-slate-900 dark:text-slate-200">{sourceLabel}</span>
      </div>

      <div className="mt-6 grid gap-4 sm:grid-cols-3">
        <div className="rounded-3xl bg-slate-50 p-4 dark:bg-slate-900/80">
          <p className="text-xs uppercase tracking-[0.3em] text-slate-500 dark:text-slate-400">Price</p>
          <p className="mt-3 whitespace-nowrap text-xl font-semibold text-slate-900 dark:text-slate-100">{hotel.price || '—'}</p>
        </div>
        <div className="rounded-3xl bg-slate-50 p-4 dark:bg-slate-900/80">
          <p className="text-xs uppercase tracking-[0.3em] text-slate-500 dark:text-slate-400">Rating</p>
          <p className="mt-3 whitespace-nowrap text-xl font-semibold text-slate-900 dark:text-slate-100">{hotel.rating || '—'}</p>
          <p className="mt-2 text-xs text-slate-600 dark:text-slate-400">Sentiment: {hotel.sentiment || 'N/A'}</p>
        </div>
        <div className="rounded-3xl bg-slate-50 p-4 dark:bg-slate-900/80">
          <p className="text-xs uppercase tracking-[0.3em] text-slate-500 dark:text-slate-400">Reviews</p>
          <p className="mt-3 whitespace-nowrap text-xl font-semibold text-slate-900 dark:text-slate-100">{formatReviewLabel(hotel.reviews)}</p>
        </div>
      </div>
    </section>
  );
}

export default HotelCard;
