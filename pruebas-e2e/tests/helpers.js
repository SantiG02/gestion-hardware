// Funciones de apoyo compartidas por todas las pruebas.
const { expect } = require('@playwright/test');

// Un identificador distinto en cada ejecución, para no chocar con datos anteriores.
const RUN = Date.now().toString().slice(-6);

const USUARIOS = {
  gestor:  { email: 'hardtrack.gestor@gmail.com',  password: 'Admin1234',   nombre: 'Gestor Tecnologico',  inicio: /\/dashboard$/ },
  tecnico: { email: 'hardtrack.tecnico@gmail.com', password: 'Tecnico1234', nombre: 'Tecnico de Soporte',  inicio: /\/panel-tecnico$/ },
  usuario: { email: 'hardtrack.usuario@gmail.com', password: 'Usuario1234', nombre: 'Usuario Final',       inicio: /\/mis-incidencias$/ },
};

const SEDE = 'Sede Pruebas PW';
const AREA = 'Area Pruebas PW';

/** Inicia sesión por la pestaña «Iniciar sesión». Acepta un rol o {email,password}. */
async function ingresar(page, quien) {
  const u = typeof quien === 'string' ? USUARIOS[quien] : quien;
  await page.goto('/login');
  await page.locator('#tab-ingresar').click();
  await page.locator('#email').fill(u.email);
  await page.locator('#password').fill(u.password);
  await page.locator('#panel-ingresar button[type=submit]').click();
  if (u.inicio) await expect(page).toHaveURL(u.inicio);
}

async function cerrarSesion(page) {
  await page.locator('.sidebar__salir').click();
  await expect(page).toHaveURL(/\/login/);
}

/** Campo de formulario (input, select o textarea) según el texto de su etiqueta. */
function campo(page, etiqueta) {
  const patron = new RegExp('^\\s*' + etiqueta.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'));
  return page
    .locator('.campo')
    .filter({ has: page.locator('label.campo__etiqueta', { hasText: patron }) })
    .locator('input:not([type=hidden]), select, textarea')
    .first();
}

/** Elige en un <select> la primera opción cuyo texto contenga `texto` (o cumpla la regex). */
async function elegir(select, texto) {
  const opciones = await select.locator('option').allTextContents();
  const i = opciones.findIndex((t) => (texto instanceof RegExp ? texto.test(t) : t.includes(texto)));
  if (i < 0) throw new Error(`No existe la opción «${texto}». Opciones: ${opciones.join(' | ')}`);
  await select.selectOption({ index: i });
}

/** Botón principal de envío del formulario de la página (no el de cerrar sesión). */
function enviar(page) {
  return page.locator('main form button[type=submit]').first();
}

/** Fila de tabla que contiene el texto indicado. */
function fila(page, texto) {
  return page.locator('tbody tr').filter({ hasText: texto });
}

/** Tarjeta (bloque) de «Mis incidencias» que contiene el texto indicado. */
function tarjeta(page, texto) {
  return page.locator('div.panel').filter({ hasText: texto }).last();
}

function fechaISO(diasDesdeHoy = 0) {
  const d = new Date();
  d.setDate(d.getDate() + diasDesdeHoy);
  const p = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}

/** Crea (si no existen) la sede y el área de pruebas. Debe llamarse como Gestor. */
async function asegurarSedeArea(page) {
  await page.goto('/sedes');
  if ((await fila(page, SEDE).count()) === 0) {
    await page.goto('/sedes/nuevo');
    await campo(page, 'Nombre').fill(SEDE);
    await campo(page, 'Ciudad').fill('Bogotá');
    await enviar(page).click();
    await expect(page).toHaveURL(/\/sedes$/);
  }
  await fila(page, SEDE).getByRole('link', { name: 'Ver áreas' }).click();
  if ((await fila(page, AREA).count()) === 0) {
    await page.getByRole('link', { name: 'Nueva área' }).click();
    await campo(page, 'Nombre del área').fill(AREA);
    await enviar(page).click();
  }
  await expect(fila(page, AREA)).toHaveCount(1);
}

/** Registra un equipo de pruebas asignado al Usuario Final. Debe llamarse como Gestor. */
async function crearEquipo(page, codigo) {
  await page.goto('/equipos/nuevo');
  await campo(page, 'Código interno').fill(codigo);
  await campo(page, 'Serial').fill('SER-' + codigo);
  await campo(page, 'Marca').fill('Dell');
  await campo(page, 'Modelo').fill('Latitude PW');
  await elegir(campo(page, 'Área'), AREA);
  await elegir(campo(page, 'Estado'), 'Estable');
  await elegir(campo(page, 'Usuario responsable'), USUARIOS.usuario.nombre);
  await campo(page, 'Fecha de compra').fill(fechaISO(-400));
  await enviar(page).click();
}

module.exports = {
  RUN, USUARIOS, SEDE, AREA,
  ingresar, cerrarSesion, campo, elegir, enviar, fila, tarjeta, fechaISO,
  asegurarSedeArea, crearEquipo,
};
