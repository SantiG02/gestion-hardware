// Configuración de las pruebas funcionales automatizadas de HardTrack.
// La aplicación debe estar corriendo (.\mvnw.cmd spring-boot:run) contra
// la base de pruebas gestion_hardware_test antes de ejecutar las pruebas.
const { defineConfig, devices } = require('@playwright/test');

module.exports = defineConfig({
  testDir: './tests',
  timeout: 45_000,
  expect: { timeout: 8_000 },
  fullyParallel: false,
  workers: 1,                       // un solo navegador: las pruebas comparten datos
  retries: 0,
  reporter: [
    ['list'],
    ['html', { outputFolder: 'reporte', open: 'never' }],
    ['json', { outputFile: 'resultados.json' }],
  ],
  use: {
    baseURL: process.env.BASE_URL || 'http://localhost:8080',
    locale: 'es-CO',
    timezoneId: 'America/Bogota',
    acceptDownloads: true,
    screenshot: 'only-on-failure',  // captura de cada fallo
    trace: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
