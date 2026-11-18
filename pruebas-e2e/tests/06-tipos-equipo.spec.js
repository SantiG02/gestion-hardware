// Módulo Tipos de equipo y campos dinámicos: el Gestor define tipos y campos propios
// y el formulario de equipos cambia según el tipo elegido.
const { test, expect } = require('@playwright/test');
const { RUN, USUARIOS, AREA, ingresar, campo, elegir, enviar, fila, fechaISO, asegurarSedeArea } = require('./helpers');

test.describe.configure({ mode: 'serial' });

const TIPO = `Switch PW ${RUN}`;
const COD = `PW-${RUN}-SW`;

test.beforeEach(async ({ page }) => { await ingresar(page, 'gestor'); });

/** Llena los datos generales de un equipo (sin tipo ni campos propios). */
async function llenarGenerales(page, codigo) {
  await campo(page, 'Código interno').fill(codigo);
  await campo(page, 'Serial').fill('SER-' + codigo);
  await campo(page, 'Marca').fill('Cisco');
  await campo(page, 'Modelo').fill('Catalyst PW');
  await elegir(campo(page, 'Área'), AREA);
  await elegir(campo(page, 'Estado'), 'Estable');
  await campo(page, 'Fecha de compra').fill(fechaISO(-200));
}

test('Preparación: sede y área de pruebas', async ({ page }) => {
  await asegurarSedeArea(page);
});

test('TE-01 Crear un tipo de equipo sin especificaciones de cómputo', async ({ page }) => {
  await page.goto('/tipos-equipo/nuevo');
  await campo(page, 'Nombre del tipo').fill(TIPO);
  await page.getByLabel('Incluye especificaciones de cómputo').uncheck();
  await enviar(page).click();
  await expect(page).toHaveURL(/\/tipos-equipo$/);
  await expect(page.locator('.aviso--exito')).toContainText('Tipo de equipo registrado correctamente');
  await expect(fila(page, TIPO)).toContainText('Activo');
});

test('TE-02 No se permite un nombre de tipo repetido', async ({ page }) => {
  await page.goto('/tipos-equipo/nuevo');
  await campo(page, 'Nombre del tipo').fill(TIPO);
  await enviar(page).click();
  await expect(page.locator('.campo__error')).toContainText('Ya existe un tipo de equipo con ese nombre');
  await page.goto('/tipos-equipo');
  await expect(fila(page, TIPO)).toHaveCount(1);
});

test('TE-03 Definir un campo numérico obligatorio', async ({ page }) => {
  await page.goto('/tipos-equipo');
  await fila(page, TIPO).getByRole('link', { name: 'Campos' }).click();
  await page.getByRole('link', { name: 'Nuevo campo' }).click();
  await campo(page, 'Nombre del campo').fill('Número de puertos');
  await elegir(campo(page, 'Tipo de dato'), 'Número');
  await page.getByLabel('Obligatorio al registrar el equipo').check();
  await enviar(page).click();
  await expect(page.locator('.aviso--exito')).toContainText('Campo registrado correctamente');
  await expect(fila(page, 'Número de puertos')).toContainText('Sí');
});

test('TE-04 Definir un campo de lista: las opciones son obligatorias', async ({ page }) => {
  await page.goto('/tipos-equipo');
  await fila(page, TIPO).getByRole('link', { name: 'Campos' }).click();
  await page.getByRole('link', { name: 'Nuevo campo' }).click();
  await campo(page, 'Nombre del campo').fill('Administrable');
  await expect(page.locator('#bloque-opciones')).toBeHidden();
  await elegir(campo(page, 'Tipo de dato'), 'Lista');
  await expect(page.locator('#bloque-opciones')).toBeVisible();

  // Sin opciones se rechaza.
  await enviar(page).click();
  await expect(page.locator('.campo__error')).toContainText('Escriba al menos una opcion');

  await campo(page, 'Opciones').fill('Sí, No');
  await enviar(page).click();
  await expect(page.locator('.aviso--exito')).toContainText('Campo registrado correctamente');
  await expect(fila(page, 'Administrable')).toContainText('Sí,No');
});

test('TE-05 No se permite un nombre de campo repetido en el mismo tipo', async ({ page }) => {
  await page.goto('/tipos-equipo');
  await fila(page, TIPO).getByRole('link', { name: 'Campos' }).click();
  await page.getByRole('link', { name: 'Nuevo campo' }).click();
  await campo(page, 'Nombre del campo').fill('Número de puertos');
  await enviar(page).click();
  await expect(page.locator('.campo__error')).toContainText('Ya existe un campo con ese nombre en este tipo');
});

test('TE-06 El formulario de equipo cambia según el tipo elegido', async ({ page }) => {
  await page.goto('/equipos/nuevo');
  const tipo = campo(page, 'Tipo de equipo');

  // Sin tipo: especificaciones de cómputo visibles, sin campos propios.
  await expect(page.locator('#bloque-basicas')).toBeVisible();
  await expect(page.getByText('Número de puertos')).toBeHidden();

  // Tipo nuevo: se ocultan las especificaciones de cómputo y aparecen sus campos.
  await elegir(tipo, TIPO);
  await expect(page.locator('#bloque-basicas')).toBeHidden();
  await expect(page.getByText('Número de puertos *')).toBeVisible();
  await expect(page.getByText('Administrable')).toBeVisible();

  // Tipo con cómputo (Escritorio): vuelve el bloque de cómputo y se van los campos propios.
  await elegir(tipo, 'Escritorio');
  await expect(page.locator('#bloque-basicas')).toBeVisible();
  await expect(page.getByText('Número de puertos')).toBeHidden();
});

test('TE-07 Un campo obligatorio vacío impide guardar el equipo', async ({ page }) => {
  await page.goto('/equipos/nuevo');
  await llenarGenerales(page, COD);
  await elegir(campo(page, 'Tipo de equipo'), TIPO);
  await enviar(page).click();
  await expect(page.locator('.campo__error')).toContainText('Este campo es obligatorio');
  await page.goto('/equipos');
  await expect(fila(page, COD)).toHaveCount(0);
});

test('TE-08 Registrar un equipo del tipo nuevo y ver sus valores en el historial', async ({ page }) => {
  await page.goto('/equipos/nuevo');
  await llenarGenerales(page, COD);
  await elegir(campo(page, 'Tipo de equipo'), TIPO);
  await campo(page, 'Número de puertos').fill('24');
  await elegir(campo(page, 'Administrable'), 'Sí');
  await enviar(page).click();
  await expect(page).toHaveURL(/\/equipos$/);
  await expect(page.locator('.aviso--exito')).toContainText('Equipo registrado correctamente');

  await fila(page, COD).getByRole('link', { name: 'Historial' }).click();
  const panel = page.locator('main');
  await expect(panel).toContainText(`Especificaciones de ${TIPO}`);
  await expect(panel).toContainText('Número de puertos');
  await expect(panel).toContainText('24');
  await expect(panel).toContainText('Administrable');
});

test('TE-09 Al editar el equipo se conservan y se pueden cambiar los valores', async ({ page }) => {
  await page.goto('/equipos');
  await fila(page, COD).getByRole('link', { name: 'Editar' }).click();
  await expect(campo(page, 'Número de puertos')).toHaveValue('24');
  await campo(page, 'Número de puertos').fill('48');
  await enviar(page).click();
  await expect(page.locator('.aviso--exito')).toContainText('Equipo actualizado correctamente');

  await fila(page, COD).getByRole('link', { name: 'Historial' }).click();
  await expect(page.locator('main')).toContainText('48');
});

test('TE-10 No se puede inactivar un tipo con equipos activos', async ({ page }) => {
  await page.goto('/tipos-equipo');
  page.once('dialog', (d) => d.accept());
  await fila(page, TIPO).getByRole('button', { name: 'Inactivar' }).click();
  await expect(page.locator('.aviso--error')).toContainText('No se puede inactivar');
  await expect(fila(page, TIPO)).toContainText('Activo');
});

test('TE-11 Un tipo inactivo ya no se ofrece al registrar equipos', async ({ page }) => {
  // Se da de baja el equipo y entonces el tipo sí se puede inactivar.
  await page.goto('/equipos');
  page.once('dialog', (d) => d.accept());
  await fila(page, COD).getByRole('button', { name: 'Dar de baja' }).click();
  await expect(page.locator('.aviso--exito')).toContainText('dado de baja');

  await page.goto('/tipos-equipo');
  page.once('dialog', (d) => d.accept());
  await fila(page, TIPO).getByRole('button', { name: 'Inactivar' }).click();
  await expect(page.locator('.aviso--exito')).toContainText('Tipo de equipo inactivado correctamente');
  await expect(fila(page, TIPO)).toContainText('Inactivo');

  await page.goto('/equipos/nuevo');
  const opciones = await campo(page, 'Tipo de equipo').locator('option').allTextContents();
  expect(opciones.some((t) => t.includes(TIPO)), 'El tipo inactivo no debe aparecer en el formulario').toBe(false);
});

test('TE-12 El Técnico y el Usuario Final no acceden a Tipos de equipo', async ({ page }) => {
  for (const rol of ['tecnico', 'usuario']) {
    await page.context().clearCookies();
    await ingresar(page, rol);
    await page.goto('/tipos-equipo');
    await expect(page.getByRole('heading', { name: 'Acceso denegado' }), `${rol} no debe entrar`).toBeVisible();
  }
});
