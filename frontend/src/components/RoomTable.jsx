function RoomTable({ rooms }) {
  const banned = ['we price match', 'breakfast included', 'filter', 'see details', 'parking', 'free wifi', 'offer', 'cancellation policy', 'original price', 'per night before taxes', 'tooltip', 'view deal'];
  const cleaned = Array.isArray(rooms) ? rooms.filter((r) => {
    const name = (r?.roomType || '').toString().toLowerCase();
    if (!name || name.length < 3) return false;
    if (banned.some((b) => name.includes(b))) return false;
    return true;
  }).map((r) => ({
    roomType: r.roomType || r.name || null,
    guests: r.guests || r.maxGuests || '—',
    price: r.price || r.rate || '—',
    breakfastIncluded: typeof r.breakfastIncluded === 'boolean' ? r.breakfastIncluded : String(r.breakfast || '').toLowerCase().includes('yes') || String(r.breakfast || '').toLowerCase().includes('included'),
    refundable: typeof r.refundable === 'boolean' ? r.refundable : String(r.refundable || '').toLowerCase().includes('yes') || String(r.cancellation || '').toLowerCase().includes('free')
  })) : [];

  if (!cleaned.length) {
    return (
      <section className="rounded-3xl border border-dashed border-slate-300/70 bg-white/95 p-8 text-center text-slate-500 shadow-sm dark:border-slate-700/70 dark:bg-slate-950/90 dark:text-slate-400">
        <p className="text-lg font-semibold">Room inventory will appear here.</p>
        <p className="mt-2 text-sm">Search a hotel to review structured room offers.</p>
      </section>
    );
  }

  return (
    <section className="rounded-3xl border border-slate-200/80 bg-white/95 p-6 shadow-sm dark:border-slate-700/80 dark:bg-slate-950/90">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm uppercase tracking-[0.3em] text-slate-500 dark:text-slate-400">Room options</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900 dark:text-slate-100">Room inventory</h2>
        </div>
      </div>
      <div className="mt-6 overflow-hidden rounded-3xl border border-slate-200 dark:border-slate-700">
        <table className="min-w-full divide-y divide-slate-200 dark:divide-slate-700 text-sm text-slate-700 dark:text-slate-200">
          <thead className="bg-slate-50 text-left uppercase tracking-[0.3em] text-slate-500 dark:bg-slate-900 dark:text-slate-400">
            <tr>
              <th className="px-4 py-3">Room Type</th>
              <th className="px-4 py-3">Guests</th>
              <th className="px-4 py-3">Price</th>
              <th className="px-4 py-3">Breakfast</th>
              <th className="px-4 py-3">Refundable</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-200 bg-white dark:divide-slate-700 dark:bg-slate-950">
            {cleaned.map((room, index) => (
              <tr key={`${room.roomType || 'room'}-${index}`} className="even:bg-slate-50 odd:bg-white dark:even:bg-slate-900 dark:odd:bg-slate-950">
                <td className="px-4 py-4">{room.roomType || 'Room option'}</td>
                <td className="px-4 py-4 whitespace-nowrap">{room.guests || '—'}</td>
                <td className="px-4 py-4 whitespace-nowrap">{room.price || '—'}</td>
                <td className="px-4 py-4 whitespace-nowrap">{room.breakfastIncluded ? 'Yes' : 'No'}</td>
                <td className="px-4 py-4 whitespace-nowrap">{room.refundable ? 'Yes' : 'No'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

export default RoomTable;
