// Convierte resultados.json (Playwright) en resultados.csv para pasarlo al Excel del plan de pruebas.
// Uso:  node exportar-resultados.js
const fs = require('fs');

const datos = JSON.parse(fs.readFileSync('resultados.json', 'utf8'));
const filas = [];

function recorrer(suite) {
  for (const spec of suite.specs || []) {
    for (const t of spec.tests || []) {
      const r = t.results[t.results.length - 1] || {};
      const estado = { passed: 'Pasó', failed: 'Falló', timedOut: 'Falló', skipped: 'Omitida', interrupted: 'Falló' }[r.status] || r.status || 'Sin ejecutar';
      const id = (spec.title.match(/^([A-Z]{2}-\d{2}[a-z]?(?:\/\d+)*)/) || [''])[0];
      const error = ((r.error && r.error.message) || '').replace(/\u001b\[[0-9;]*m/g, '').split('\n')[0];
      const nota = (t.annotations || []).map((a) => a.description).filter(Boolean).join(' ');
      filas.push([id, spec.title, estado, error || nota]);
    }
  }
  for (const s of suite.suites || []) recorrer(s);
}
(datos.suites || []).forEach(recorrer);

const esc = (v) => '"' + String(v ?? '').replace(/"/g, '""') + '"';
const csv = ['ID;Prueba;Estado;Detalle', ...filas.map((f) => f.map(esc).join(';'))].join('\n');
fs.writeFileSync('resultados.csv', '﻿' + csv, 'utf8');

const cuenta = filas.reduce((a, f) => ((a[f[2]] = (a[f[2]] || 0) + 1), a), {});
console.log('resultados.csv generado:', cuenta);
