import { defineConfig, devices } from '@playwright/test';

// Las pruebas funcionales levantan su propio backend (perfil e2e: H2 en memoria, limpia en cada ejecución)
// y su propio frontend, en puertos distintos a los de desarrollo para no interferir con la app abierta.
const PUERTO_BACKEND = 8081;
const PUERTO_FRONTEND = 5174;
const mvnw = process.platform === 'win32' ? '.\\mvnw.cmd' : './mvnw';

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: `http://localhost:${PUERTO_FRONTEND}`,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: [
    {
      command: `${mvnw} -q spring-boot:run -Dspring-boot.run.profiles=e2e -Dspring-boot.run.arguments=--server.port=${PUERTO_BACKEND}`,
      cwd: '../backend',
      url: `http://localhost:${PUERTO_BACKEND}/api/partidas/historico`,
      timeout: 240_000,
      reuseExistingServer: false,
      stdout: 'ignore',
      stderr: 'pipe',
    },
    {
      command: `npx vite --port ${PUERTO_FRONTEND} --strictPort`,
      url: `http://localhost:${PUERTO_FRONTEND}`,
      timeout: 60_000,
      reuseExistingServer: false,
      env: { BACKEND_URL: `http://localhost:${PUERTO_BACKEND}` },
    },
  ],
});
