// Módulo Importación masiva de equipos desde CSV y Excel (RF-07, RF-08).
const { test, expect } = require('@playwright/test');
const ExcelJS = require('exceljs');
const { RUN, AREA, ingresar, fila, asegurarSedeArea } = require('./helpers');

test.describe.configure({ mode: 'serial' });

const ENCABEZADO = 'Código interno*,Serial*,Marca*,Modelo*,Tipo de equipo,Área*,Procesador,RAM (GB),' +
  'Tipo de almacenamiento,Capacidad (GB),Sistema operativo,Fecha de compra*,Fecha puesta en operación,Observaciones';

/** Una fila del archivo, en el orden de la plantilla. */
function linea({ cod, serial, marca = 'Dell', modelo = 'Optiplex PW', tipo = 'Escritorio', area = AREA,
  cpu = 'Intel Core i5', ram = '8', alm = 'SSD', cap = '256', so = 'Windows 11',
  compra = '15/01/2024', puesta = '20/01/2024', obs = 'Importado por prueba' }) {
  return [cod, serial, marca, modelo, tipo, area, cpu, ram, alm, cap, so, compra, puesta, obs].join(',');
}

const csv = (...lineas) => [ENCABEZADO, ...lineas].join('\n') + '\n';

const BUENOS = [1, 2, 3].map((n) => ({ cod: `IMP-${RUN}-${n}`, serial: `SERIMP-${RUN}-${n}` }));

async function subir(page, nombre, mimeType, buffer) {
  await page.goto('/equipos/importar');
  await page.locator('#archivo').setInputFiles({ name: nombre, mimeType, buffer });
  await page.getByRole('button', { name: 'Importar equipos' }).click();
  await expect(page.getByRole('heading', { name: 'Resultado de la importación' })).toBeVisible();
}

/** Cifra del resultado («Filas leídas», «Cargados», «Rechazados»). */
const dato = (page, etiqueta) =>
  page.locator('p').filter({ has: page.locator('.campo__etiqueta', { hasText: etiqueta }) });

test.beforeEach(async ({ page }) => { await ingresar(page, 'gestor'); });

test('Preparación: sede y área de pruebas', async ({ page }) => {
  await asegurarSedeArea(page);
});

test('IM-01 Descargar las plantillas CSV y Excel', async ({ page }) => {
  const rCsv = await page.request.get('/equipos/importar/plantilla.csv');
  expect(rCsv.status()).toBe(200);
  expect(await rCsv.text()).toContain('Serial*');

  const rXlsx = await page.request.get('/equipos/importar/plantilla.xlsx');
  expect(rXlsx.status()).toBe(200);
  const libro = new ExcelJS.Workbook();
  await libro.xlsx.load(await rXlsx.body());
  expect(libro.worksheets[0].getRow(1).getCell(2).value).toContain('Serial');
});

test('IM-02 Importar un CSV válido carga todos los equipos', async ({ page }) => {
  await subir(page, 'equipos-validos.csv', 'text/csv',
    Buffer.from(csv(...BUENOS.map((b) => linea(b))), 'utf-8'));

  await expect(dato(page, 'Filas leídas')).toHaveText(/3/);
  await expect(dato(page, 'Cargados')).toHaveText(/3/);
  await expect(dato(page, 'Rechazados')).toHaveText(/0/);
  await expect(page.getByText('Todos los registros se cargaron correctamente')).toBeVisible();

  // Los equipos aparecen en el inventario con el estado inicial.
  await page.goto('/equipos');
  for (const b of BUENOS) {
    await expect(fila(page, b.cod)).toHaveCount(1);
    await expect(fila(page, b.cod)).toContainText('Estable');
  }
});

test('IM-03 Un CSV con filas inválidas rechaza solo esas filas y explica el motivo', async ({ page }) => {
  const e = (n) => ({ cod: `IMP-${RUN}-E${n}`, serial: `SERIMP-${RUN}-E${n}` });
  await subir(page, 'equipos-con-errores.csv', 'text/csv', Buffer.from(csv(
    linea(e(1)),                                                   // válida
    linea({ cod: e(1).cod, serial: `SERIMP-${RUN}-E1B` }),         // código repetido en el archivo
    linea({ ...e(3), area: 'Area Que No Existe' }),                // área inexistente
    linea({ ...e(4), tipo: 'Tipo Fantasma' }),                     // tipo inexistente
    linea({ ...e(5), compra: 'ayer' }),                            // fecha inválida
    linea({ cod: e(6).cod, serial: '' }),                          // falta el serial
  ), 'utf-8'));

  await expect(dato(page, 'Filas leídas')).toHaveText(/6/);
  await expect(dato(page, 'Cargados')).toHaveText(/1/);
  await expect(dato(page, 'Rechazados')).toHaveText(/5/);

  await expect(fila(page, e(1).cod)).toContainText('Código interno repetido en el archivo');
  await expect(fila(page, e(3).cod)).toContainText("El área 'Area Que No Existe' no existe");
  await expect(fila(page, e(4).cod)).toContainText("El tipo de equipo 'Tipo Fantasma' no existe");
  await expect(fila(page, e(5).cod)).toContainText('Fecha de compra inválida');
  await expect(fila(page, e(6).cod)).toContainText('Falta el serial');

  // Solo la fila válida llegó al inventario.
  await page.goto('/equipos');
  await expect(fila(page, e(1).cod)).toHaveCount(1);
  await expect(fila(page, e(3).cod)).toHaveCount(0);
});

test('IM-04 Importar de nuevo el mismo archivo no duplica equipos', async ({ page }) => {
  await subir(page, 'equipos-validos.csv', 'text/csv',
    Buffer.from(csv(...BUENOS.map((b) => linea(b))), 'utf-8'));

  await expect(dato(page, 'Cargados')).toHaveText(/0/);
  await expect(dato(page, 'Rechazados')).toHaveText(/3/);
  await expect(fila(page, BUENOS[0].cod)).toContainText('Ya existe un equipo con ese código interno');

  await page.goto('/equipos');
  await expect(fila(page, BUENOS[0].cod)).toHaveCount(1);
});

test('IM-05 Importar un Excel (.xlsx) basado en la plantilla', async ({ page }) => {
  const r = await page.request.get('/equipos/importar/plantilla.xlsx');
  const libro = new ExcelJS.Workbook();
  await libro.xlsx.load(await r.body());
  const hoja = libro.worksheets[0];

  const datos = [1, 2].map((n) => linea({ cod: `IMX-${RUN}-${n}`, serial: `SERIMX-${RUN}-${n}`, marca: 'HP' }).split(','));
  datos[0].forEach((v, i) => { hoja.getRow(2).getCell(i + 1).value = v; });  // reemplaza la fila de ejemplo
  hoja.addRow(datos[1]);

  const buffer = Buffer.from(await libro.xlsx.writeBuffer());
  await subir(page, 'equipos.xlsx',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', buffer);

  await expect(dato(page, 'Filas leídas')).toHaveText(/2/);
  await expect(dato(page, 'Cargados')).toHaveText(/2/);
  await expect(dato(page, 'Rechazados')).toHaveText(/0/);

  await page.goto('/equipos');
  await expect(fila(page, `IMX-${RUN}-1`)).toHaveCount(1);
  await expect(fila(page, `IMX-${RUN}-2`)).toHaveCount(1);
});

test('IM-06 El Técnico y el Usuario Final no acceden a la importación', async ({ page }) => {
  for (const rol of ['tecnico', 'usuario']) {
    await page.context().clearCookies();
    await ingresar(page, rol);
    await page.goto('/equipos/importar');
    await expect(page.getByRole('heading', { name: 'Acceso denegado' }), `${rol} no debe entrar`).toBeVisible();
  }
});
