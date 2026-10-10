// Módulo Acceso: crear cuenta, iniciar sesión, recuperar contraseña, cerrar sesión.
const { test, expect } = require('@playwright/test');
const { RUN, USUARIOS, ingresar, cerrarSesion } = require('./helpers');


const NUEVO = { nombre: 'Cuenta Prueba ' + RUN, email: `pw.${RUN}@prueba.com`, password: 'Prueba1234' };

/** ¿La base está vacía (sin Gestor)? Entonces el formulario dice «Crear administrador». */
async function baseSinGestor(page) {
  await page.goto('/login');
  return (await page.locator('#panel-crear h2').first().textContent()).includes('administrador');
}

async function abrirCrear(page) {
  await page.goto('/login');
  await page.locator('#tab-crear').click();
}

async function llenarCrear(page, d, { terminos = true, confirmacion } = {}) {
  await page.locator('#crear-nombre').fill(d.nombre);
  await page.locator('#crear-email').fill(d.email);
  await page.locator('#crear-password').fill(d.password);
  await page.locator('#crear-confirmacion').fill(confirmacion ?? d.password);
  if (terminos) await page.locator('#aceptaTerminos').check();
}

const botonCrear = (page) => page.locator('#panel-crear form button[type=submit]');

test.describe('Primer arranque (solo con la base de datos vacía)', () => {
  test('AC-01 Primer arranque sin administrador', async ({ page }) => {
    test.skip(!(await baseSinGestor(page)), 'La base ya tiene un Gestor: para probar AC-01 a AC-05 use una base vacía.');
    await expect(page.locator('#panel-crear h2').first()).toHaveText('Crear administrador');
    await expect(page.locator('#tab-crear')).toHaveAttribute('aria-selected', 'true');
  });

  test('AC-02 Crear administrador con contraseñas distintas', async ({ page }) => {
    test.skip(!(await baseSinGestor(page)), 'La base ya tiene un Gestor.');
    await llenarCrear(page, { nombre: 'Gestor Tecnologico', email: USUARIOS.gestor.email, password: USUARIOS.gestor.password }, { confirmacion: 'Otra12345' });
    await botonCrear(page).click();
    await expect(page.locator('.aviso--error')).toContainText('Las contraseñas no coinciden');
  });

  test('AC-03 Crear administrador con contraseña corta', async ({ page }) => {
    test.skip(!(await baseSinGestor(page)), 'La base ya tiene un Gestor.');
    await page.goto('/login');
    await llenarCrear(page, { nombre: 'Gestor Tecnologico', email: USUARIOS.gestor.email, password: 'corta' });
    const valido = await page.locator('#crear-password').evaluate((el) => el.checkValidity());
    expect(valido, 'El navegador debe rechazar contraseñas de menos de 8 caracteres').toBe(false);
  });

  test('AC-04 Crear administrador sin aceptar términos', async ({ page }) => {
    test.skip(!(await baseSinGestor(page)), 'La base ya tiene un Gestor.');
    await page.goto('/login');
    await llenarCrear(page, { nombre: 'Gestor Tecnologico', email: USUARIOS.gestor.email, password: USUARIOS.gestor.password }, { terminos: false });
    const valido = await page.locator('#aceptaTerminos').evaluate((el) => el.checkValidity());
    expect(valido, 'La casilla de términos debe ser obligatoria').toBe(false);
  });

  test('AC-05 Crear el primer administrador', async ({ page }) => {
    test.skip(!(await baseSinGestor(page)), 'La base ya tiene un Gestor.');
    await page.goto('/login');
    await llenarCrear(page, { nombre: 'Gestor Tecnologico', email: USUARIOS.gestor.email, password: USUARIOS.gestor.password });
    await botonCrear(page).click();
    await expect(page).toHaveURL(/\/login/);
    await expect(page.locator('.aviso--exito').first()).toBeVisible();
    await ingresar(page, 'gestor');
  });
});

test('AC-06 Después del primer administrador el formulario dice «Crear cuenta»', async ({ page }) => {
  await abrirCrear(page);
  await expect(page.locator('#panel-crear h2').first()).toHaveText('Crear cuenta');
});

test('AC-07 Ver términos y condiciones', async ({ page }) => {
  await abrirCrear(page);
  const [popup] = await Promise.all([
    page.waitForEvent('popup'),
    page.getByRole('link', { name: 'Términos y Condiciones' }).click(),
  ]);
  await expect(popup).toHaveURL(/\/terminos$/);
  await expect(popup.locator('h1')).toHaveText('Términos y Condiciones');
});

test('AC-14 Correo ya registrado', async ({ page }) => {
  await abrirCrear(page);
  await llenarCrear(page, { nombre: 'Duplicado', email: USUARIOS.usuario.email, password: 'Prueba1234' });
  await botonCrear(page).click();
  await expect(page.locator('.aviso--error')).toContainText('Ya existe una cuenta con este correo');
});

test('AC-11/15/17 Crear cuenta nueva: queda como Usuario Final y puede ingresar', async ({ page }) => {
  await abrirCrear(page);
  await llenarCrear(page, NUEVO);
  await botonCrear(page).click();

  // Si Brevo está configurado, el sistema pide el código por correo: esa parte es manual.
  const pideCodigo = await page.getByText('Confirma tu correo').isVisible().catch(() => false);
  test.skip(pideCodigo, 'Brevo está configurado: la cuenta exige código por correo. Pruebe AC-08 a AC-13 a mano, o ejecute sin la clave de Brevo.');

  await expect(page).toHaveURL(/\/login/);
  await expect(page.locator('.aviso--exito').first()).toContainText('Cuenta creada');
  await ingresar(page, { email: NUEVO.email, password: NUEVO.password, inicio: /\/mis-incidencias$/ });
  await expect(page.locator('h1.encabezado__titulo')).toHaveText('Mis incidencias');
});

test('AC-18 Inicio de sesión con contraseña incorrecta', async ({ page }) => {
  await page.goto('/login');
  await page.locator('#tab-ingresar').click();
  await page.locator('#email').fill(USUARIOS.usuario.email);
  await page.locator('#password').fill('ClaveIncorrecta9');
  await page.locator('#panel-ingresar button[type=submit]').click();
  await expect(page).toHaveURL(/\/login\?error/);
  await expect(page.locator('.aviso--error')).toContainText('no coinciden');
});

test('AC-21 Recuperar con correo inexistente muestra el mismo mensaje genérico', async ({ page }) => {
  const mensaje = async (correo) => {
    await page.goto('/recuperar');
    await page.locator('#email').fill(correo);
    await page.getByRole('button', { name: 'Enviar enlace de recuperación' }).click();
    await expect(page.locator('h2.acceso__titulo')).toHaveText('Revisa tu correo');
    return (await page.locator('.acceso__intro').textContent()).trim();
  };
  const inexistente = await mensaje(`no.existe.${RUN}@prueba.com`);
  const existente = await mensaje(USUARIOS.usuario.email);
  expect(inexistente).toBe(existente); // no revela si la cuenta existe
});

test('AC-22 Enlace de recuperación inválido', async ({ page }) => {
  await page.goto('/restablecer?token=token-que-no-existe');
  await expect(page.locator('h2.acceso__titulo')).toHaveText('Enlace vencido o inválido');
});

test('AC-23 Cerrar sesión y no poder volver con «atrás»', async ({ page }) => {
  await ingresar(page, 'usuario');
  await cerrarSesion(page);
  await expect(page.locator('.aviso--exito')).toContainText('Cerraste sesión correctamente');
  await page.goBack();
  await expect(page.locator('.sidebar')).toHaveCount(0); // no se ve el menú de la aplicación
});
