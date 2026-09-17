package co.edu.uan.gestionhardware.dto;

/**
 * Un segmento del donut de distribucion de estados. La geometria ya viene
 * calculada desde el servicio como un arco SVG real (atributo "d" de un
 * <path>), en vez del truco de circulo con stroke-dasharray, que en varios
 * navegadores deja ver un corte justo en los 4 puntos cardinales.
 */
public class SegmentoDonut {

    private final String nombre;
    private final long cantidad;
    private final String colorVar;
    private final double porcentaje;
    private final String pathArco;

    public SegmentoDonut(String nombre, long cantidad, String colorVar, double porcentaje, String pathArco) {
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.colorVar = colorVar;
        this.porcentaje = porcentaje;
        this.pathArco = pathArco;
    }

    public String getNombre() { return nombre; }
    public long getCantidad() { return cantidad; }
    public String getColorVar() { return colorVar; }
    public double getPorcentaje() { return porcentaje; }
    public String getPathArco() { return pathArco; }
}