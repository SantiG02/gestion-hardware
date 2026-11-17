(function () {
    var mes = document.getElementById('mes');
    var tipo = document.getElementById('tipo');
    var campoMes = document.getElementById('campoMes');

    var mesActual = mes.getAttribute('data-mes-actual');
    if (mesActual) {
        mes.value = mesActual;
    }

    function alternarMes() {
        campoMes.style.display = tipo.value === 'MENSUAL' ? 'block' : 'none';
    }

    tipo.addEventListener('change', alternarMes);
    alternarMes();
})();
