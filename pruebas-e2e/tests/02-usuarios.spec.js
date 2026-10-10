// Módulo Usuarios: crear, editar rol, inactivar/activar y efecto en el acceso.
const { test, expect } = require('@playwright/test');
const { RUN, ingresar, campo, elegir, enviar, fila } = require('./helpers');

test.describe.configure({ mode: 'serial' });

const U = { nombre: 'Usuario Gestionado ' + RUN, email: `gestionado.${RUN}@prueba.com`, password: 'Gestion1234' };

test.beforeEach(async ({ page }) => { await ingresar(page, 'gestor'); });

test('US-01 Crear usuario desde Usuarios', async ({ page }) => {
  await page.goto('/usuarios/nuevo');
  await campo(page, 'Nombre completo').fill(U.nombre);
  await campo(page, 'Correo electrónico').fill(U.email);
  await campo(page, 'Contraseña').fill(U.password);
  await elegir(campo(page, 'Rol'), /Usuario/i);
  await enviar(page).click();
  await expect(page).toHaveURL(/\/usuarios$/);
  await expect(page.locator('.aviso--exito')).toContainText('Usuario registrado correctamente');
  await expect(fila(page, U.email)).toHaveCount(1);
});

test('RP-05 Cambiar el rol de un usuario (Usuario Final → Técnico)', async ({ page }) => {
  await page.goto('/usuarios');
  await fila(page, U.email).getByRole('link', { name: 'Editar' }).click();
  await elegir(campo(page, 'Rol'), /T[eé]cnico/i);
  await enviar(page).click();
  await expect(page.locator('.aviso--exito')).toContainText('Usuario actualizado correctamente');
  // Al reingresar ve el panel del Técnico.
  await page.context().clearCookies();
  await ingresar(page, { email: U.email, password: U.password, inicio: /\/panel-tecnico$/ });
});

test('US-03 Inactivar usuario', async ({ page }) => {
  await page.goto('/usuarios');
  page.once('dialog', (d) => d.accept());
  await fila(page, U.email).getByRole('button', { name: 'Inactivar' }).click();
  await expect(page.locator('.aviso--exito')).toContainText('inactivado');
  await expect(fila(page, U.email).getByRole('button', { name: 'Activar' })).toBeVisible();
});

test('AC-19 Un usuario inactivo no puede ingresar', async ({ page }) => {
  await page.context().clearCookies();
  await page.goto('/login');
  await page.locator('#tab-ingresar').click();
  await page.locator('#email').fill(U.email);
  await page.locator('#password').fill(U.password);
  await page.locator('#panel-ingresar button[type=submit]').click();
  await expect(page).toHaveURL(/\/login\?error/);
  await expect(page.locator('.aviso--error')).toBeVisible();
});

test('US-03b Activar usuario devuelve el acceso', async ({ page }) => {
  await page.goto('/usuarios');
  page.once('dialog', (d) => d.accept());
  await fila(page, U.email).getByRole('button', { name: 'Activar' }).click();
  await expect(page.locator('.aviso--exito')).toContainText('activado');
  await page.context().clearCookies();
  await ingresar(page, { email: U.email, password: U.password, inicio: /\/panel-tecnico$/ });
});
