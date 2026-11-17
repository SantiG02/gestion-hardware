package co.edu.uan.gestionhardware.model;

/**
 * Tipos de dato que el gestor puede elegir al definir un campo propio
 * de un tipo de equipo.
 */
public enum TipoDatoCampo {

    TEXTO("Texto"),
    NUMERO("Número"),
    FECHA("Fecha"),
    LISTA("Lista de opciones");

    private final String etiqueta;

    TipoDatoCampo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }
}
