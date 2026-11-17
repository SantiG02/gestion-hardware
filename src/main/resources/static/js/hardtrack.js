// Comportamiento comun a todas las pantallas (sin scripts en linea,
// para poder usar una Content-Security-Policy estricta).

// Confirmacion antes de enviar formularios marcados con data-confirm="mensaje".
document.addEventListener('submit', function (evento) {
    var formulario = evento.target;
    var mensaje = formulario.getAttribute && formulario.getAttribute('data-confirm');
    if (mensaje && !window.confirm(mensaje)) {
        evento.preventDefault();
    }
});
