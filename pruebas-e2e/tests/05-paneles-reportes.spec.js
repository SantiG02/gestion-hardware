// Dashboard, paneles por rol, alertas y reportes.
const { test, expect } = require('@playwright/test');
const fs = require('fs');
const { ingresar } = require('./helpers');

const kpi = (page, etiqueta) =>
  page.locator('.kpi').filter({ hasText: etiqueta }).locator('.kpi__valor').first();

test('DB-01 Indicadores del Gestor coherentes con la lista de equipos', async ({ page }) => {
  await ingresar(page, 'gestor');
  for (const e of ['Equipos activos', 'Incidencias abiertas', 'Mantenimientos pendientes', 'Candidatos']) {
    await expect(page.locator('.kpi').filter({ hasText: e }).first()).toBeVisible();
  }
  const tarjeta = parseInt((await kpi(page, 'Equipos activos').textContent()).trim(), 10);
  await page.goto('/equipos');
  const filas = await page.locator('tbody tr').count();
  expect(tarjeta, 'La tarjeta «Equipos activos» debe coincidir con las filas de /equipos').toBe(filas);
});

test('DB-02 Alertas activas', async ({ page }) => {
  await ingresar(page, 'gestor');
  await expect(page.getByRole('heading', { name: 'Alertas activas' })).toBeVisible();
  const conAlertas = await page.locator('.alerta-item').count();
  if (conAlertas === 0) {
    await expect(page.getByText('No hay alertas activas en este momento')).toBeVisible();
  } else {
    await expect(page.locator('.alerta-item').first()).toBeVisible();
  }
});

test('DB-03 Panel del Técnico', async ({ page }) => {
  await ingresar(page, 'tecnico');
  for (const e of ['Mis mantenimientos pendientes', 'Incidencias abiertas', 'Mantenimientos por vencer', 'Equipos activos']) {
    await expect(page.locator('.kpi__etiqueta', { hasText: e })).toBeVisible();
  }
});

test('DB-04 Panel del Usuario Final', async ({ page }) => {
  await ingresar(page, 'usuario');
  for (const e of ['Total reportadas', 'Abiertas', 'Esperando tu confirmación', 'Confirmadas']) {
    await expect(page.locator('.kpi__etiqueta', { hasText: e })).toBeVisible();
  }
});

test('AL-01 (parcial) El botón «Enviar alertas por correo» responde sin error', async ({ page }) => {
  // La llegada real del correo se verifica a mano. Aquí se comprueba que el sistema responde bien.
  await ingresar(page, 'gestor');
  await page.getByRole('button', { name: 'Enviar alertas por correo' }).click();
  await expect(page).toHaveURL(/\/dashboard/);
  await expect(page.getByText(/Whitelabel|Internal Server Error/i)).toHaveCount(0);
  await expect(page.locator('.aviso').first()).toBeVisible();
});

async function descargar(page, boton, extension) {
  const [descarga] = await Promise.all([
    page.waitForEvent('download'),
    page.getByRole('button', { name: boton }).click(),
  ]);
  expect(descarga.suggestedFilename().toLowerCase()).toMatch(new RegExp('\\.' + extension + '$'));
  const bytes = fs.statSync(await descarga.path()).size;
  expect(bytes, 'El archivo descargado está vacío').toBeGreaterThan(1000);
}

test('RE-01 Reporte PDF', async ({ page }) => {
  await ingresar(page, 'gestor');
  await page.goto('/reportes');
  await page.locator('#tipo').selectOption('ANUAL');
  await descargar(page, 'Descargar PDF', 'pdf');
});

test('RE-02 Reporte Excel', async ({ page }) => {
  await ingresar(page, 'gestor');
  await page.goto('/reportes');
  await page.locator('#tipo').selectOption('ANUAL');
  await descargar(page, 'Descargar Excel', 'xlsx');
});
