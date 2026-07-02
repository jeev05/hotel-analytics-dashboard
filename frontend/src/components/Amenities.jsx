function Amenities({ hotel }) {
  const rawAmenities = hotel?.amenities || [];
  const policies = hotel?.policies || [];
  const houseRules = hotel?.houseRules || [];
  const roomFeatures = hotel?.roomFeatures || [];

  // filter out junk / UI labels that may have been scraped into amenities.
  // NOTE: 'parking' and 'free wifi' are real amenities and must not be banned here —
  // they were previously included by mistake, which silently dropped them from every listing.
  const banned = ['reviews', 'location', 'policies', 'tooltip', 'hotel name', 'price', 'address', 'view deal', 'offer', 'filter', 'see details', 'original price'];
  const amenities = rawAmenities.filter((a) => {
    if (!a || typeof a !== 'string') return false;
    const text = a.trim().toLowerCase();
    if (text.length < 3) return false;
    if (banned.some((b) => text.includes(b))) return false;
    return true;
  });

  return (
    <section className="rounded-3xl border border-slate-200/80 bg-white/95 p-6 shadow-sm dark:border-slate-700/80 dark:bg-slate-950/90">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <p className="text-sm uppercase tracking-[0.3em] text-slate-500 dark:text-slate-400">Amenities & policies</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900 dark:text-slate-100">Guest experience</h2>
        </div>
        <p className="text-sm text-slate-600 dark:text-slate-400">Only structured amenities and policy items are shown.</p>
      </div>

      <div className="mt-6 grid gap-4 lg:grid-cols-[1.1fr_0.9fr]">
        <div className="rounded-3xl bg-slate-50 p-5 dark:bg-slate-900/80">
          <p className="text-sm uppercase tracking-[0.3em] text-slate-500 dark:text-slate-400">Amenities</p>
          {amenities.length ? (
            <div className="mt-4 grid gap-3 sm:grid-cols-2">
              {amenities.map((amenity, index) => (
                <span key={index} className="inline-flex items-center justify-center rounded-full bg-white px-3 py-2 text-sm font-medium text-slate-900 shadow-sm dark:bg-slate-950/80 dark:text-slate-100">
                  ✓ {amenity}
                </span>
              ))}
            </div>
          ) : (
            <div className="mt-4 rounded-3xl bg-white p-4 text-sm text-slate-500 shadow-sm dark:bg-slate-950/80 dark:text-slate-400">
              No structured amenities available.
            </div>
          )}
        </div>

        <div className="grid gap-4">
          {policies.length ? (
            <div className="rounded-3xl bg-slate-50 p-5 dark:bg-slate-900/80">
              <p className="text-sm uppercase tracking-[0.3em] text-slate-500 dark:text-slate-400">Policies</p>
              <ul className="mt-4 space-y-3 text-sm text-slate-700 dark:text-slate-300">
                {policies.map((policy, index) => (
                  <li key={index} className="rounded-3xl bg-white p-4 shadow-sm dark:bg-slate-950/80">{policy}</li>
                ))}
              </ul>
            </div>
          ) : null}

          {roomFeatures.length ? (
            <div className="rounded-3xl bg-slate-50 p-5 dark:bg-slate-900/80">
              <p className="text-sm uppercase tracking-[0.3em] text-slate-500 dark:text-slate-400">Room features</p>
              <ul className="mt-4 space-y-3 text-sm text-slate-700 dark:text-slate-300">
                {roomFeatures.map((feature, index) => (
                  <li key={index} className="rounded-3xl bg-white p-4 shadow-sm dark:bg-slate-950/80">{feature}</li>
                ))}
              </ul>
            </div>
          ) : null}

          {houseRules.length ? (
            <div className="rounded-3xl bg-slate-50 p-5 dark:bg-slate-900/80">
              <p className="text-sm uppercase tracking-[0.3em] text-slate-500 dark:text-slate-400">House rules</p>
              <ul className="mt-4 space-y-3 text-sm text-slate-700 dark:text-slate-300">
                {houseRules.map((rule, index) => (
                  <li key={index} className="rounded-3xl bg-white p-4 shadow-sm dark:bg-slate-950/80">{rule}</li>
                ))}
              </ul>
            </div>
          ) : null}
        </div>
      </div>
    </section>
  );
}

export default Amenities;
