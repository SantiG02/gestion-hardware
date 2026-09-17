package co.edu.uan.gestionhardware.dto;

import java.util.List;

/**
 * Resultado de procesar un archivo de importacion masiva (RF-07, RF-08):
 * cuantas filas traia el archivo, cuantas se cargaron y el detalle de las
 * que se rechazaron.
 */
public class ResultadoImportacion {

    private final String nombreArchivo;
    private final int totalFilas;
    private final int cargados;
    private final List<FilaInconsistente> inconsistencias;

    public ResultadoImportacion(String nombreArchivo, int totalFilas, int cargados,
                                List<FilaInconsistente> inconsistencias) {
        this.nombreArchivo = nombreArchivo;
        this.totalFilas = totalFilas;
        this.cargados = cargados;
        this.inconsistencias = inconsistencias;
    }

    public String getNombreArchivo() { return nombreArchivo; }
    public int getTotalFilas() { return totalFilas; }
    public int getCargados() { return cargados; }
    public int getRechazados() { return inconsistencias.size(); }
    public List<FilaInconsistente> getInconsistencias() { return inconsistencias; }
}