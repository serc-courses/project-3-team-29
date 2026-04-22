import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Routes that belong to the React SPA (not the backend API).
// When the browser navigates to these paths (Accept: text/html), Vite must
// serve index.html instead of proxying to the backend.
const SPA_PREFIXES = ['/orders/new', '/orders/', '/funds/', '/advisor/', '/account', '/login']

function isSpaNavigation(req) {
  // Browser page-load requests include 'text/html' in Accept
  const accept = req.headers['accept'] || ''
  return accept.includes('text/html')
}

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5174,
    proxy: {
      '/orders': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        bypass(req) {
          if (isSpaNavigation(req)) return '/index.html'
        },
      },
      '/funds': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        bypass(req) {
          if (isSpaNavigation(req)) return '/index.html'
        },
      },
      '/view': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/auth': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/accounts': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        bypass(req) {
          if (isSpaNavigation(req)) return '/index.html'
        },
      },
      '/advisor': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        bypass(req) {
          if (isSpaNavigation(req)) return '/index.html'
        },
      },
    },
  },
})
