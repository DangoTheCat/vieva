/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: "class",
  theme: {
    extend: {
      colors: {
        sidebarBg: '#0B132B',
        sidebarHover: '#1C2541',
        canvasBg: '#F1F5F9',
        cardBg: '#FFFFFF',
        accentSky: '#0284C7',
        accentNavy: '#0F172A',
        emeraldCustom: '#10B981',
        amberCustom: '#F59E0B',
        roseCustom: '#EF4444',
        brand: {
          50: '#F0F7FF',
          100: '#E0EFFF',
          500: '#0066FF',
          600: '#0052CC',
          700: '#003D99',
        }
      },
      fontFamily: {
        sans: ['"Be Vietnam Pro"', '"Inter"', 'sans-serif'],
        heading: ['"Plus Jakarta Sans"', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'monospace']
      }
    },
  },
  plugins: [],
}
