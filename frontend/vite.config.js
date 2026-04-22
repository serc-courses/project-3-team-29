import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

function isSpaNavigation(req) {
  const accept = req.headers['accept'] || ''
  return accept.includes('text/html')
}

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/orders': {
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
      '/funds': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        bypass(req) {
          if (isSpaNavigation(req)) return '/index.html'
        },
      },
      '/transfer-agent': {
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
