function ImageGallery({ images, hotelName }) {
  return (
    <section className="rounded-3xl border border-slate-200/80 bg-white/90 p-6 shadow-lg dark:border-slate-700/80 dark:bg-slate-900/90">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-sm uppercase tracking-[0.3em] text-slate-500 dark:text-slate-400">Hotel image gallery</p>
          <h2 className="mt-2 text-2xl font-semibold text-slate-900 dark:text-slate-100">Visual portfolio</h2>
        </div>
        <p className="text-sm text-slate-600 dark:text-slate-400">Preview curated imagery for {hotelName || 'selected hotel'}.</p>
      </div>

      <div className="mt-6 grid gap-4 sm:grid-cols-2">
        {images.length ? (
          images.map((src, index) => (
            <div key={index} className="group overflow-hidden rounded-3xl bg-slate-950/5 shadow-sm transition hover:-translate-y-1 hover:shadow-lg dark:bg-slate-950">
              <img
                src={src}
                alt={`${hotelName || 'hotel'} preview ${index + 1}`}
                className="h-[220px] w-full object-cover transition duration-500 group-hover:scale-105"
              />
            </div>
          ))
        ) : (
          <div className="rounded-3xl border border-dashed border-slate-300/70 bg-slate-50 p-10 text-center text-slate-500 dark:border-slate-700/70 dark:bg-slate-900/90 dark:text-slate-400">
            <p className="text-base">Search a hotel to reveal the image gallery.</p>
          </div>
        )}
      </div>
    </section>
  );
}

export default ImageGallery;
