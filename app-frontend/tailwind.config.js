/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{js,jsx,ts,tsx}",
  ],
  theme: {
    extend: {
      // Palette de couleurs personnalisée
      colors: {
        // Couleur de la sidebar (bleu marine foncé)
        sidebar: {
          DEFAULT: '#1e3a8a',
          dark:    '#172d6e',
          active:  '#2563eb',
        },
        // Couleur principale de l'application (bleu)
        primary: {
          DEFAULT: '#2563eb',
          light:   '#3b82f6',
          dark:    '#1d4ed8',
        },
        // Couleur de fond général (gris très clair)
        background: '#f1f5f9',
      },
    },
  },
  plugins: [],
}
