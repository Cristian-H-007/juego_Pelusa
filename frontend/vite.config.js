import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// El frontend llama a /api/... y Vite redirige esas peticiones al backend Spring Boot (puerto 8080).
const backend = process.env.BACKEND_URL ?? 'http://localhost:8080';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
    proxy: { '/api': backend },
  },
  preview: {
    port: 5173,
    strictPort: true,
    proxy: { '/api': backend },
  },
});
