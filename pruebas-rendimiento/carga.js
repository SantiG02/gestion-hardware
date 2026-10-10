// Prueba de carga de HardTrack con k6.
// Sube de 10 a 25 y a 50 usuarios concurrentes. Cada nivel dura DURACION (5m por defecto).
// Mezcla de usuarios: 20 % Gestor, 30 % Técnico, 50 % Usuario Final.
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const DURACION = __ENV.DURACION || '5m';
const NIVELES = [10, 25, 50];

const CRED = {
  gestor:  { email: __ENV.GESTOR_EMAIL  || 'hardtrack.gestor@gmail.com',  pass: __ENV.GESTOR_PASS  || 'Admin1234' },
  tecnico: { email: __ENV.TECNICO_EMAIL || 'hardtrack.tecnico@gmail.com', pass: __ENV.TECNICO_PASS || 'Tecnico1234' },
  usuario: { email: __ENV.USUARIO_EMAIL || 'hardtrack.usuario@gmail.com', pass: __ENV.USUARIO_PASS || 'Usuario1234' },
};

function segundos(texto) {
  const n = parseInt(texto, 10);
  if (texto.endsWith('m')) return n * 60;
  if (texto.endsWith('h')) return n * 3600;
  return n;
}

// Los usuarios no inician sesión todos en el mismo segundo: se escalonan.
const ESCALONADO = Math.min(10, segundos(DURACION) / 4);

const escenarios = {};
const umbrales = {
  http_req_failed: ['rate<0.01'],
  http_req_duration: ['p(95)<2000'],
};

NIVELES.forEach((n, indice) => {
  const g = Math.round(n * 0.2);
  const t = Math.round(n * 0.3);
  const u = n - g - t;
  const inicio = `${indice * segundos(DURACION)}s`;
  [['gestor', g], ['tecnico', t], ['usuario', u]].forEach(([rol, vus]) => {
    escenarios[`n${n}_${rol}`] = {
      executor: 'constant-vus',
      vus,
      duration: DURACION,
      startTime: inicio,
      exec: rol,
      tags: { nivel: String(n) },
    };
  });
  umbrales[`http_req_duration{nivel:${n}}`] = ['p(95)<2000'];
  umbrales[`http_req_failed{nivel:${n}}`] = ['rate<0.01'];
});

['login', 'dashboard', 'equipos', 'equipos_busqueda', 'incidencias', 'mantenimientos',
 'panel_tecnico', 'mis_incidencias', 'reportar_incidencia'].forEach((pagina) => {
  umbrales[`http_req_duration{name:${pagina}}`] = ['p(95)<2000'];
});

export const options = {
  scenarios: escenarios,
  thresholds: umbrales,
  noCookiesReset: true, // la sesión debe sobrevivir entre iteraciones
};

// Estado propio de cada usuario virtual
let rolActual = null;
let opcionesEquipo = null;
let opcionesCategoria = null;

function iniciarSesion(rol) {
  const c = CRED[rol];
  const pagina = http.get(`${BASE}/login`, { tags: { name: 'login_pagina' } });
  const csrf = pagina.html().find('input[name=_csrf]').first().attr('value');
  const r = http.post(`${BASE}/login`,
    { email: c.email, password: c.pass, _csrf: csrf },
    { tags: { name: 'login' } });
  const ok = check(r, { 'inicio de sesión correcto': (x) => x.status === 200 && !x.url.includes('/login') });
  if (ok) rolActual = rol;
  return ok;
}

function asegurarSesion(rol) {
  if (rolActual === rol) return true;
  if (rolActual === null) sleep(Math.random() * ESCALONADO);
  return iniciarSesion(rol);
}

function ver(ruta, nombre) {
  const r = http.get(`${BASE}${ruta}`, { tags: { name: nombre } });
  check(r, { [`${nombre}: responde 200 y con sesión`]: (x) => x.status === 200 && !x.url.includes('/login') });
  sleep(1 + Math.random() * 2); // tiempo de lectura de una persona
  return r;
}

export function gestor() {
  if (!asegurarSesion('gestor')) { sleep(5); return; }
  ver('/dashboard', 'dashboard');
  ver('/equipos', 'equipos');
  ver('/equipos?texto=PERF-00' + Math.floor(Math.random() * 10), 'equipos_busqueda');
  ver('/incidencias', 'incidencias');
  ver('/mantenimientos', 'mantenimientos');
}

export function tecnico() {
  if (!asegurarSesion('tecnico')) { sleep(5); return; }
  ver('/panel-tecnico', 'panel_tecnico');
  ver('/equipos', 'equipos');
  ver('/incidencias', 'incidencias');
  ver('/mantenimientos', 'mantenimientos');
}

function valoresDeSelect(html, nombre) {
  const valores = [];
  html.find(`select[name=${nombre}] option`).each((_, opcion) => {
    const v = opcion.attr('value');
    if (v) valores.push(v);
  });
  return valores;
}

export function usuario() {
  if (!asegurarSesion('usuario')) { sleep(5); return; }
  ver('/mis-incidencias', 'mis_incidencias');

  // Cada 5 iteraciones reporta una incidencia nueva (flujo completo con formulario).
  if (__ITER % 5 === 0) {
    const form = http.get(`${BASE}/mis-incidencias/nueva`, { tags: { name: 'reportar_incidencia' } });
    const html = form.html();
    if (!opcionesEquipo) {
      opcionesEquipo = valoresDeSelect(html, 'equipo');
      opcionesCategoria = valoresDeSelect(html, 'categoriaFalla');
    }
    const csrf = html.find('input[name=_csrf]').first().attr('value');
    if (opcionesEquipo.length === 0 || opcionesCategoria.length === 0) { sleep(3); return; }

    sleep(2 + Math.random() * 3); // tiempo de llenar el formulario
    const r = http.post(`${BASE}/mis-incidencias/guardar`, {
      equipo: opcionesEquipo[Math.floor(Math.random() * opcionesEquipo.length)],
      categoriaFalla: opcionesCategoria[Math.floor(Math.random() * opcionesCategoria.length)],
      prioridad: 'MEDIA',
      descripcion: 'Prueba de carga: el equipo presenta una falla',
      _csrf: csrf,
    }, { tags: { name: 'reportar_incidencia' } });
    check(r, { 'incidencia reportada': (x) => x.status === 200 && x.url.endsWith('/mis-incidencias') });
    sleep(1);
  }
}