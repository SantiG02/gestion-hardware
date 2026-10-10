// Módulo Roles y permisos.
const { test, expect } = require('@playwright/test');
const { USUARIOS, ingresar } = require('./helpers');

test('RP-01 Redirección por rol después de iniciar sesión', async ({ page }) => {
  for (const rol of ['gestor', 'tecnico', 'usuario']) {
    await ingresar(page, rol); // ingresar() ya verifica la URL de destino de cada rol
    await page.context().clearCookies();
  }
});

test('RP-02 Usuario Final sin acceso a inventario', async ({ page }) => {
  await ingresar(page, 'usuario');
  await page.goto('/equipos');
  await expect(page.locator('h1.encabezado__titulo')).toHaveText('Acceso denegado');
});

test('RP-03 Técnico sin acceso a usuarios', async ({ page }) => {
  await ingresar(page, 'tecnico');
  await page.goto('/usuarios');
  await expect(page.locator('h1.encabezado__titulo')).toHaveText('Acceso denegado');
});

test('RP-04 Sin sesión se redirige al login', async ({ page }) => {
  await page.goto('/dashboard');
  await expect(page).toHaveURL(/\/login/);
});

// Matriz completa: cada rol solo entra a las rutas que le corresponden.
const RUTAS = [
  // [ruta, gestor, tecnico, usuario]
  ['/dashboard',        true,  false, false],
  ['/usuarios',         true,  false, false],
  ['/sedes',            true,  false, false],
  ['/configuracion',    true,  false, false],
  ['/reportes',         true,  false, false],
  ['/equipos',          true,  true,  false],
  ['/incidencias',      true,  true,  false],
  ['/mantenimientos',   true,  true,  false],
  ['/panel-tecnico',    false, true,  false],
  ['/mis-incidencias',  false, false, true],
];

test('RP-02b Matriz de rutas permitidas y prohibidas por rol', async ({ page }) => {
  const errores = [];
  for (const [i, rol] of ['gestor', 'tecnico', 'usuario'].entries()) {
    await ingresar(page, rol);
    for (const ruta of RUTAS) {
      const permitido = ruta[i + 1];
      await page.goto(ruta[0]);
      const denegado = await page.getByRole('heading', { name: 'Acceso denegado' }).isVisible().catch(() => false);
      if (permitido === denegado) {
        errores.push(`${rol} en ${ruta[0]}: se esperaba ${permitido ? 'acceso' : 'acceso denegado'}`);
      }
    }
    await page.context().clearCookies();
  }
  expect(errores, errores.join('\n')).toEqual([]);
});
