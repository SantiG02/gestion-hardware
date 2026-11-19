(function () {
    var pestanas = [
        { boton: document.getElementById('tab-ingresar'), panel: document.getElementById('panel-ingresar') },
        { boton: document.getElementById('tab-crear'),    panel: document.getElementById('panel-crear') }
    ];
    function mostrar(indice) {
        pestanas.forEach(function (p, i) {
            var activa = i === indice;
            p.boton.setAttribute('aria-selected', activa ? 'true' : 'false');
            p.panel.hidden = !activa;
        });
        var campo = pestanas[indice].panel.querySelector('input:not([tabindex="-1"])');
        if (campo) { campo.focus(); }
    }
    pestanas.forEach(function (p, i) {
        p.boton.addEventListener('click', function () { mostrar(i); });
    });
})();

// Evita el doble envio de los formularios (doble toque en el celular): el
// segundo POST llegaba con el token CSRF ya renovado y terminaba en error.
(function () {
    document.querySelectorAll('form').forEach(function (formulario) {
        formulario.addEventListener('submit', function (evento) {
            if (formulario.dataset.enviado === 'true') {
                evento.preventDefault();
                return;
            }
            formulario.dataset.enviado = 'true';
            setTimeout(function () {
                formulario.querySelectorAll('button[type="submit"], input[type="submit"]')
                    .forEach(function (boton) { boton.disabled = true; });
            }, 0);
        });
    });
    window.addEventListener('pageshow', function (evento) {
        if (!evento.persisted) { return; }
        document.querySelectorAll('form').forEach(function (formulario) {
            formulario.dataset.enviado = 'false';
            formulario.querySelectorAll('button[type="submit"], input[type="submit"]')
                .forEach(function (boton) { boton.disabled = false; });
        });
    });
})();
