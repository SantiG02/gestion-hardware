// Formulario de equipos dinamico: segun el tipo elegido se muestran las
// especificaciones de computo (si el tipo las usa) y los campos propios
// que el gestor definio para ese tipo.
(function () {
    var selector = document.getElementById('tipoEquipo');
    var bloqueBasicas = document.getElementById('bloque-basicas');
    var grupos = document.querySelectorAll('[data-grupo-tipo]');

    if (!selector || !bloqueBasicas) {
        return;
    }

    function habilitar(contenedor, activo) {
        contenedor.hidden = !activo;
        contenedor.querySelectorAll('input, select, textarea').forEach(function (campo) {
            // Un campo deshabilitado no se envia con el formulario.
            campo.disabled = !activo;
        });
    }

    function actualizar() {
        var opcion = selector.options[selector.selectedIndex];
        var tipoId = selector.value;
        var usaBasicas = !tipoId || !opcion || opcion.getAttribute('data-basicas') !== 'false';

        habilitar(bloqueBasicas, usaBasicas);

        grupos.forEach(function (grupo) {
            habilitar(grupo, grupo.getAttribute('data-grupo-tipo') === tipoId);
        });
    }

    selector.addEventListener('change', actualizar);
    actualizar();
})();
