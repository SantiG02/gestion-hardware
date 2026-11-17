var zona = document.getElementById('zonaCarga');
var entrada = document.getElementById('archivo');

['dragover', 'dragenter'].forEach(function (evento) {
    zona.addEventListener(evento, function (e) {
        e.preventDefault();
        zona.classList.add('zona-carga--activa');
    });
});

['dragleave', 'drop'].forEach(function (evento) {
    zona.addEventListener(evento, function (e) {
        e.preventDefault();
        zona.classList.remove('zona-carga--activa');
    });
});

zona.addEventListener('drop', function (e) {
    if (e.dataTransfer.files.length > 0) {
        entrada.files = e.dataTransfer.files;
    }
});
