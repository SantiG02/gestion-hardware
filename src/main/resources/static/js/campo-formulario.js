// Muestra el cuadro de opciones solo cuando el campo es de tipo "Lista".
(function () {
    var selector = document.getElementById('tipoDato');
    var bloque = document.getElementById('bloque-opciones');

    if (!selector || !bloque) {
        return;
    }

    function actualizar() {
        bloque.hidden = selector.value !== 'LISTA';
    }

    selector.addEventListener('change', actualizar);
    actualizar();
})();
