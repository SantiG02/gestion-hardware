package co.edu.uan.gestionhardware.dto;

/**
 * Resumen de un area para la barra apilada del dashboard: cuantos equipos
 * tiene en cada estado, y el porcentaje de cada uno ya calculado (0-100)
 * para poder usarlo directo como ancho de barra en la plantilla.
 */
public class AreaResumen {

    private final String nombre;
    private final long totalEquipos;
    private final long estable;
    private final long seguimiento;
    private final long renovacion;
    private final double pctEstable;
    private final double pctSeguimiento;
    private final double pctRenovacion;

    public AreaResumen(String nombre, long totalEquipos, long estable, long seguimiento, long renovacion,
                       double pctEstable, double pctSeguimiento, double pctRenovacion) {
        this.nombre = nombre;
        this.totalEquipos = totalEquipos;
        this.estable = estable;
        this.seguimiento = seguimiento;
        this.renovacion = renovacion;
        this.pctEstable = pctEstable;
        this.pctSeguimiento = pctSeguimiento;
        this.pctRenovacion = pctRenovacion;
    }

    public String getNombre() { return nombre; }
    public long getTotalEquipos() { return totalEquipos; }
    public long getEstable() { return estable; }
    public long getSeguimiento() { return seguimiento; }
    public long getRenovacion() { return renovacion; }
    public double getPctEstable() { return pctEstable; }
    public double getPctSeguimiento() { return pctSeguimiento; }
    public double getPctRenovacion() { return pctRenovacion; }
}