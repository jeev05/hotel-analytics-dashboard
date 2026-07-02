export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      boxShadow: {
        glow: '0 20px 60px rgba(15, 23, 42, 0.18)'
      },
      backgroundImage: {
        'dashboard-surface': 'radial-gradient(circle at top, rgba(59, 130, 246, 0.12), transparent 40%)'
      }
    }
  },
  plugins: []
};
