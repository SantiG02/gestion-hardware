// Módulo Equipos (y sedes/áreas que necesita).
const { test, expect } = require('@playwright/test');
const { RUN, ingresar, campo, elegir, enviar, fila, asegurarSedeArea, crearEquipo, AREA } = require('./helpers');

test.describe.configure({ mode: 'serial' });

const COD = `PW-${RUN}-A`;

test.beforeEach(async ({ page }) => { await ingresar(page, 'gestor'); });

test('Preparación: sede y área de pruebas', async ({ page }) => {
  await asegurarSedeArea(page);
});

test('EQ-01 Registrar equipo', async ({ page }) => {
  await crearEquipo(page, COD);
  await expect(page).toHaveURL(/\/equipos$/);
  await expect(page.locator('.aviso--exito')).toContainText('Equipo registrado correctamente');
  const f = fila(page, COD);
  await expect(f).toHaveCount(1);
  await expect(f).toContainText('Estable'); // estado calculado
});

test('EQ-02 Código duplicado no crea un segundo equipo', async ({ page }) => {
  await crearEquipo(page, COD);
  const html = await page.content();
  expect(html, 'El sistema mostró una pantalla de error al duplicar el código').not.toMatch(/Whitelabel|Internal Server Error|status=500/i);
  await page.goto('/equipos');
  await expect(fila(page, COD)).toHaveCount(1);
});

test('EQ-03 Editar equipo', async ({ page }) => {
  await page.goto('/equipos');
  await fila(page, COD).getByRole('link', { name: 'Editar' }).click();
  await campo(page, 'Modelo').fill('Latitude PW editado');
  await enviar(page).click();
  await expect(page.locator('.aviso--exito')).toContainText('Equipo actualizado correctamente');
  await expect(fila(page, COD)).toContainText('Latitude PW editado');
});

test('EQ-05 Filtros de equipos', async ({ page }) => {
  await page.goto('/equipos');
  const total = await page.locator('tbody tr').count();
  await page.locator('input[name=texto]').fill(COD);
  await page.getByRole('button', { name: 'Filtrar' }).click();
  await expect(page.locator('tbody tr')).toHaveCount(1);
  await page.getByRole('link', { name: 'Limpiar' }).click();
  await expect(page.locator('tbody tr')).toHaveCount(total);
});

test('EQ-08 Historial del equipo', async ({ page }) => {
  await page.goto('/equipos');
  await fila(page, COD).getByRole('link', { name: 'Historial' }).click();
  await expect(page).toHaveURL(/\/equipos\/\d+\/historial$/);
  await expect(page.locator('main')).toContainText(COD);
});

test('EQ-04 Desactivar equipo', async ({ page }) => {
  await page.goto('/equipos');
  page.once('dialog', (d) => d.accept());
  await fila(page, COD).getByRole('button', { name: 'Dar de baja' }).click();
  await expect(page.locator('.aviso--exito')).toContainText('dado de baja');
  await expect(fila(page, COD)).toHaveCount(0);
});
