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
      '/auth': {
        target: 'http://localhost',
        changeOrigin: true,
      },
      '/orders': {
        target: 'http://localhost',
        changeOrigin: true,
        bypass(req) {
          if (isSpaNavigation(req)) return '/index.html'
        },
      },
      '/view': {
        target: 'http://localhost',
        changeOrigin: true,
      },
      '/funds': {
        target: 'http://localhost',
        changeOrigin: true,
        bypass(req) {
          if (isSpaNavigation(req)) return '/index.html'
        },
      },
      '/transfer-agent': {
        target: 'http://localhost',
        changeOrigin: true,
      },
      '/accounts': {
        target: 'http://localhost',
        changeOrigin: true,
        bypass(req) {
          if (isSpaNavigation(req)) return '/index.html'
        },
      },
      '/advisor': {
        target: 'http://localhost',
        changeOrigin: true,
        bypass(req) {
          if (isSpaNavigation(req)) return '/index.html'
        },
      },
    },
  },
})
