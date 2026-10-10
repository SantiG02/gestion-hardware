// AC-16: límite de cuentas por hora desde el mismo equipo.
// Ejecutar al final (el límite se reinicia al reiniciar la aplicación).
const { test, expect } = require('@playwright/test');
const { RUN } = require('./helpers');

test('AC-16 Límite de intentos de crear cuenta por equipo', async ({ page }) => {
  let mensaje = null;
  for (let i = 1; i <= 8 && !mensaje; i++) {
    await page.goto('/login');
    await page.locator('#tab-crear').click();
    await page.locator('#crear-nombre').fill('Limite ' + i);
    await page.locator('#crear-email').fill(`limite.${RUN}.${i}@prueba.com`);
    await page.locator('#crear-password').fill('Prueba1234');
    await page.locator('#crear-confirmacion').fill('Prueba1234');
    await page.locator('#aceptaTerminos').check();
    await page.locator('#panel-crear form button[type=submit]').click();

    if (await page.getByText('Confirma tu correo').isVisible().catch(() => false)) {
      test.skip(true, 'Brevo está configurado: esta prueba enviaría correos reales. Ejecútela sin la clave de Brevo.');
    }
    const error = page.locator('.aviso--error');
    if (await error.count()) {
      const texto = await error.first().textContent();
      if (texto.includes('demasiadas')) mensaje = texto;
    }
  }
  expect(mensaje, 'Después de 5 intentos debía aparecer el mensaje de «demasiadas cuentas»').toContain('demasiadas');
});
