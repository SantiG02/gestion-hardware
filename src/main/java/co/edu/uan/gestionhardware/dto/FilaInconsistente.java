package co.edu.uan.gestionhardware.dto;

/**
 * Fila rechazada durante una importacion masiva (RF-08): numero de fila en
 * el archivo, el codigo interno que traia (si se pudo leer) y el motivo
 * del rechazo.
 */
public class FilaInconsistente {

    private final int numeroFila;
    private final String codigoInterno;
    private final String motivo;

    public FilaInconsistente(int numeroFila, String codigoInterno, String motivo) {
        this.numeroFila = numeroFila;
        this.codigoInterno = codigoInterno;
        this.motivo = motivo;
    }

    public int getNumeroFila() { return numeroFila; }
    public String getCodigoInterno() { return codigoInterno; }
    public String getMotivo() { return motivo; }
}