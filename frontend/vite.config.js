import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Forward /api calls to Spring Boot so the browser sees one origin in dev.
    // The Origin header is dropped: through the proxy the call is same-origin, so it must not be
    // judged by the backend's CORS rules (which would reject it if Vite picks another port).
    proxy: {
      '/api': {
        target: 'http://localhost:9998',
        configure: (proxy) => {
          proxy.on('proxyReq', (proxyReq) => proxyReq.removeHeader('origin'))
        },
      },
    },
  },
})
