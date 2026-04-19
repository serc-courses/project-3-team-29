import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5174,
    proxy: {
      '/orders': {
        target: 'http://localhost:8080',
        changeOrigin: true,
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
      },
      '/advisor': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
