// Copyright (c) 2026 nullata
// Licensed under the Elastic License 2.0
// See the LICENSE file in the project root for details.
/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./nullInvoice/src/main/resources/templates/**/*.html",
    "./nullInvoice/src/main/resources/static/js/**/*.js",
  ],
  theme: {
    extend: {
      colors: {
        "custom-bg": "rgb(var(--color-bg))",
        "custom-light": "rgb(var(--color-light))",
        "custom-lighter": "rgb(var(--color-lighter))",
        "custom-dark": "rgb(var(--color-dark))",
        "custom-darker": "rgb(var(--color-darker))",
        "custom-muted": "rgb(var(--color-muted))",
      },
    },
  },
  plugins: [],
};
