function StatsCards({ hotel, consistency }) {
  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
      <div className="min-h-[180px] rounded-3xl border border-slate-200/80 bg-white/95 p-5 shadow-sm dark:border-slate-700/80 dark:bg-slate-950/90">
        <p className="text-xs uppercase tracking-[0.3em] text-slate-500 dark:text-slate-400">Price</p>
        <p className="mt-4 whitespace-nowrap text-3xl font-semibold text-slate-900 dark:text-slate-100">{hotel?.price || '—'}</p>
        <p className="mt-3 text-sm text-slate-600 dark:text-slate-400">Current listing price</p>
      </div>
      <div className="min-h-[180px] rounded-3xl border border-slate-200/80 bg-white/95 p-5 shadow-sm dark:border-slate-700/80 dark:bg-slate-950/90">
        <p className="text-xs uppercase tracking-[0.3em] text-slate-500 dark:text-slate-400">Rating</p>
        <p className="mt-4 whitespace-nowrap text-3xl font-semibold text-slate-900 dark:text-slate-100">{hotel?.rating || '—'}</p>
        <p className="mt-3 text-sm text-slate-600 dark:text-slate-400">Guest score</p>
      </div>
      <div className="min-h-[180px] rounded-3xl border border-slate-200/80 bg-white/95 p-5 shadow-sm dark:border-slate-700/80 dark:bg-slate-950/90">
        <p className="text-xs uppercase tracking-[0.3em] text-slate-500 dark:text-slate-400">Reviews</p>
        <p className="mt-4 whitespace-nowrap text-3xl font-semibold text-slate-900 dark:text-slate-100">{hotel?.reviews != null ? hotel.reviews : '—'}</p>
        <p className="mt-3 text-sm text-slate-600 dark:text-slate-400">Total review count</p>
      </div>
      <div className="min-h-[180px] rounded-3xl border border-slate-200/80 bg-white/95 p-5 shadow-sm dark:border-slate-700/80 dark:bg-slate-950/90">
        <p className="text-xs uppercase tracking-[0.3em] text-slate-500 dark:text-slate-400">Image score</p>
        <p className="mt-4 whitespace-nowrap text-3xl font-semibold text-slate-900 dark:text-slate-100">{typeof hotel?.imageScore === 'number' ? `${Math.round(hotel.imageScore)} / 100` : '—'}</p>
        <p className="mt-3 text-sm text-slate-600 dark:text-slate-400">Visual listing quality</p>
      </div>
      <div className="min-h-[180px] rounded-3xl border border-slate-200/80 bg-white/95 p-5 shadow-sm dark:border-slate-700/80 dark:bg-slate-950/90">
        <p className="text-xs uppercase tracking-[0.3em] text-slate-500 dark:text-slate-400">Listing consistency</p>
        <p className="mt-4 whitespace-nowrap text-3xl font-semibold text-slate-900 dark:text-slate-100">{typeof consistency === 'number' ? `${consistency} / 100` : '—'}</p>
        <p className="mt-3 text-sm text-slate-600 dark:text-slate-400">Cross-OTA consistency score</p>
      </div>
    </div>
  );
}

export default StatsCards;
