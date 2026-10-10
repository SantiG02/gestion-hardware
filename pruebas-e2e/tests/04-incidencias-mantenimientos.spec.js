// Ciclo completo de una incidencia, mantenimientos y efecto de los umbrales.
const { test, expect } = require('@playwright/test');
const {
  RUN, USUARIOS, ingresar, campo, elegir, enviar, fila, tarjeta, fechaISO,
  asegurarSedeArea, crearEquipo,
} = require('./helpers');

test.describe.configure({ mode: 'serial' });

const COD = `PW-${RUN}-B`;
const DESC_A = `Pantalla parpadea (borrar) ${RUN}`;
const DESC_B = `El equipo no enciende ${RUN}`;
let idIncidencia;

test('Preparación: equipo de pruebas asignado al Usuario Final', async ({ page }) => {
  await ingresar(page, 'gestor');
  await asegurarSedeArea(page);
  await crearEquipo(page, COD);
  await expect(fila(page, COD)).toHaveCount(1);
});

async function reportar(page, descripcion) {
  await page.goto('/mis-incidencias/nueva');
  await elegir(campo(page, 'Equipo'), COD);
  await elegir(campo(page, 'Tipo de falla'), 'Pantalla');
  await campo(page, 'Prioridad').selectOption('ALTA');
  await campo(page, 'Describe el problema').fill(descripcion);
  await enviar(page).click();
}

test.describe('Incidencias — Usuario Final', () => {
  test.beforeEach(async ({ page }) => { await ingresar(page, 'usuario'); });

  test('IN-01 Reportar incidencia', async ({ page }) => {
    await reportar(page, DESC_A);
    await expect(page).toHaveURL(/\/mis-incidencias$/);
    await expect(page.locator('.aviso--exito')).toBeVisible();
    await expect(tarjeta(page, DESC_A)).toContainText('Abierta');
  });

  test('IN-02 Editar incidencia propia abierta', async ({ page }) => {
    await page.goto('/mis-incidencias');
    await tarjeta(page, DESC_A).getByRole('link', { name: 'Editar' }).click();
    await campo(page, 'Describe el problema').fill(DESC_A + ' (editada)');
    await enviar(page).click();
    await expect(tarjeta(page, DESC_A + ' (editada)')).toBeVisible();
  });

  test('IN-03 Eliminar incidencia propia abierta', async ({ page }) => {
    await page.goto('/mis-incidencias');
    page.once('dialog', (d) => d.accept());
    await tarjeta(page, DESC_A).getByRole('button', { name: 'Eliminar' }).click();
    await expect(page.getByText(DESC_A)).toHaveCount(0);
  });

  test('IN-01b Reportar la incidencia que seguirá el ciclo', async ({ page }) => {
    await reportar(page, DESC_B);
    await expect(tarjeta(page, DESC_B)).toContainText('Abierta');
  });
});

async function cerrarComoTecnico(page, solucion) {
  await page.goto('/incidencias');
  await fila(page, COD).getByRole('link', { name: 'Cerrar' }).click();
  await campo(page, 'Solución aplicada').fill(solucion);
  await enviar(page).click();
  await expect(page.locator('.aviso--exito')).toContainText('Incidencia cerrada correctamente');
}

test.describe('Incidencias — ciclo con el Técnico', () => {
  test('IN-04 El Técnico cierra la incidencia con una solución', async ({ page }) => {
    await ingresar(page, 'tecnico');
    await cerrarComoTecnico(page, 'Se reemplazó la fuente de poder');
    await expect(fila(page, COD)).toContainText('esperando confirmación');
  });

  test('IN-06 El Usuario Final rechaza la solución y la incidencia se reabre', async ({ page }) => {
    await ingresar(page, 'usuario');
    await page.goto('/mis-incidencias');
    await tarjeta(page, DESC_B).getByRole('link', { name: /Sigue fallando/ }).click();
    await page.locator('main textarea').fill('Sigue sin encender después de la reparación');
    await enviar(page).click();
    await expect(tarjeta(page, DESC_B)).toContainText('Abierta');
    await page.context().clearCookies();
    await ingresar(page, 'tecnico');
    await page.goto('/incidencias');
    await expect(fila(page, COD)).toContainText('ABIERTA');
  });

  test('IN-04b El Técnico vuelve a cerrar la incidencia', async ({ page }) => {
    await ingresar(page, 'tecnico');
    await cerrarComoTecnico(page, 'Se cambió la placa base');
  });

  test('IN-05 El Usuario Final aprueba la solución', async ({ page }) => {
    await ingresar(page, 'usuario');
    await page.goto('/mis-incidencias');
    const t = tarjeta(page, DESC_B);
    const href = await t.getByRole('link', { name: /Sigue fallando/ }).getAttribute('href');
    idIncidencia = href.match(/mis-incidencias\/(\d+)\//)[1];
    await t.getByRole('button', { name: /Quedó resuelto/ }).click();
    await expect(tarjeta(page, DESC_B)).toContainText('Confirmaste que quedó resuelto');
  });

  test('IN-07 No se puede editar una incidencia cerrada', async ({ page }) => {
    await ingresar(page, 'usuario');
    await page.goto('/mis-incidencias');
    await expect(tarjeta(page, DESC_B).getByRole('link', { name: 'Editar' })).toHaveCount(0);
    await page.goto(`/mis-incidencias/${idIncidencia}/editar`); // también por URL directa
    await expect(page).toHaveURL(/\/mis-incidencias$/);
  });

  test('IN-08 Filtros de incidencias', async ({ page }) => {
    await ingresar(page, 'tecnico');
    await page.goto('/incidencias');
    await page.locator('input[name=texto]').fill(COD);
    await page.getByRole('button', { name: 'Filtrar' }).click();
    await expect(page.locator('tbody tr').first()).toContainText(COD);
    for (const f of await page.locator('tbody tr').all()) await expect(f).toContainText(COD);
    await page.getByRole('link', { name: 'Limpiar' }).click();
    await expect(page.locator('input[name=texto]')).toHaveValue('');
    await page.locator('select[name=estado]').selectOption('ABIERTA');
    await page.getByRole('button', { name: 'Filtrar' }).click();
    for (const f of await page.locator('tbody tr').all()) await expect(f).toContainText('ABIERTA');
  });
});

test.describe('Mantenimientos', () => {
  test.beforeEach(async ({ page }) => { await ingresar(page, 'tecnico'); });

  test('MT-01 Programar mantenimiento', async ({ page }) => {
    await page.goto('/mantenimientos/nuevo');
    await elegir(campo(page, 'Equipo'), COD);
    await campo(page, 'Tipo').selectOption('PREVENTIVO');
    await elegir(campo(page, 'Técnico responsable'), USUARIOS.tecnico.nombre);
    await campo(page, 'Fecha programada').fill(fechaISO(3));
    await enviar(page).click();
    await expect(page).toHaveURL(/\/mantenimientos$/);
    await expect(fila(page, COD)).toContainText('PROGRAMADO');
  });

  test('MT-03 Filtros de mantenimientos', async ({ page }) => {
    await page.goto('/mantenimientos');
    await page.locator('select[name=tipo]').selectOption('PREVENTIVO');
    await page.locator('select[name=estado]').selectOption('PROGRAMADO');
    await page.getByRole('button', { name: 'Filtrar' }).click();
    await expect(fila(page, COD)).toHaveCount(1);
    await page.locator('select[name=tipo]').selectOption('CORRECTIVO');
    await page.getByRole('button', { name: 'Filtrar' }).click();
    await expect(fila(page, COD)).toHaveCount(0);
  });

  test('MT-02 Finalizar mantenimiento', async ({ page }) => {
    await page.goto('/mantenimientos');
    await fila(page, COD).getByRole('link', { name: 'Finalizar' }).click();
    await page.locator('select[name=resultado]').selectOption('EXITOSO');
    await page.locator('textarea[name=actividades]').fill('Limpieza interna y actualización de controladores');
    await page.locator('main form button[type=submit]').click();
    await expect(page.locator('.aviso--exito')).toContainText('Mantenimiento finalizado correctamente');
    await expect(fila(page, COD)).toContainText('FINALIZADO');
  });
});

test.describe('Configuración de umbrales', () => {
  let original;

  test('CF-01 Cambiar el umbral de fallas recalcula el estado de los equipos', async ({ page }) => {
    await ingresar(page, 'gestor');
    await page.goto('/configuracion');
    const entrada = campo(page, 'Fallas en 30 días');
    original = await entrada.inputValue();
    await entrada.fill('1');
    await enviar(page).click();
    await expect(page.locator('.aviso--exito')).toContainText('Configuracion actualizada correctamente');
    // El equipo de pruebas tuvo una incidencia reciente: con umbral 1 debe quedar «En seguimiento» o peor.
    await page.goto('/dashboard'); // el estado se recalcula al abrir el dashboard
    await page.goto('/equipos');
    await expect(fila(page, COD)).not.toContainText('Estable');
  });

  test.afterAll(async ({ browser }) => {
    // Restaura el umbral original aunque la prueba haya fallado.
    if (!original) return;
    const page = await browser.newPage({ baseURL: process.env.BASE_URL || 'http://localhost:8080' });
    await ingresar(page, 'gestor');
    await page.goto('/configuracion');
    await campo(page, 'Fallas en 30 días').fill(original);
    await enviar(page).click();
    await page.close();
  });
});
