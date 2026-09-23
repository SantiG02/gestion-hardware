package co.edu.uan.gestionhardware.dto;

/**
 * Resumen de las incidencias reportadas especificamente por usuarios con
 * rol Usuario Final: cuantas ha reportado, cuantas siguen abiertas, cuantas
 * quedaron cerradas esperando su confirmacion, cuantas confirmo como
 * resueltas, y cuantas veces rechazo una solucion.
 */
public class IndicadoresUsuarioFinal {

    private final long total;
    private final long abiertas;
    private final long pendientesConfirmacion;
    private final long confirmadas;
    private final long vecesRechazada;

    public IndicadoresUsuarioFinal(long total, long abiertas, long pendientesConfirmacion,
                                   long confirmadas, long vecesRechazada) {
        this.total = total;
        this.abiertas = abiertas;
        this.pendientesConfirmacion = pendientesConfirmacion;
        this.confirmadas = confirmadas;
        this.vecesRechazada = vecesRechazada;
    }

    public long getTotal() { return total; }
    public long getAbiertas() { return abiertas; }
    public long getPendientesConfirmacion() { return pendientesConfirmacion; }
    public long getConfirmadas() { return confirmadas; }
    public long getVecesRechazada() { return vecesRechazada; }
}