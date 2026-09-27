import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  // Load environment variables from the .env file
  const env = loadEnv(mode, process.cwd(), '');

  return {
    plugins: [react()],
    server: {
      // Parse the port from the .env file, fallback to 5173 if missing
      port: parseInt(env.PORT) || 5173,
      strictPort: true,
    }
  }
})