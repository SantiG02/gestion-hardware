package co.edu.uan.gestionhardware.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "incidencia")
public class Incidencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Debe seleccionar un equipo")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipo_id", nullable = false)
    private Equipo equipo;

    @NotNull(message = "Debe seleccionar la categoria de la falla")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_falla_id", nullable = false)
    private CategoriaFalla categoriaFalla;

    @NotNull(message = "Debe indicar quien reporta la incidencia")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reportado_por_id", nullable = false)
    private Usuario reportadoPor;

    @NotNull(message = "La fecha de reporte es obligatoria")
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @Column(name = "fecha_reporte", nullable = false)
    private LocalDateTime fechaReporte;

    @NotBlank(message = "La descripcion es obligatoria")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    @NotBlank(message = "Debe seleccionar la prioridad")
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String prioridad;

    @NotNull
    @Column(name = "genera_indisponibilidad", nullable = false)
    private Boolean generaIndisponibilidad = false;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @Column(name = "fecha_inicio_indisponibilidad")
    private LocalDateTime fechaInicioIndisponibilidad;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @Column(name = "fecha_fin_indisponibilidad")
    private LocalDateTime fechaFinIndisponibilidad;

    @Column(name = "horas_indisponibilidad", precision = 8, scale = 2)
    private BigDecimal horasIndisponibilidad;

    @NotBlank(message = "El estado es obligatorio")
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String estado = "ABIERTA";

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    @Column(columnDefinition = "TEXT")
    private String solucion;

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Equipo getEquipo() { return equipo; }
    public void setEquipo(Equipo equipo) { this.equipo = equipo; }

    public CategoriaFalla getCategoriaFalla() { return categoriaFalla; }
    public void setCategoriaFalla(CategoriaFalla categoriaFalla) { this.categoriaFalla = categoriaFalla; }

    public Usuario getReportadoPor() { return reportadoPor; }
    public void setReportadoPor(Usuario reportadoPor) { this.reportadoPor = reportadoPor; }

    public LocalDateTime getFechaReporte() { return fechaReporte; }
    public void setFechaReporte(LocalDateTime fechaReporte) { this.fechaReporte = fechaReporte; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String prioridad) { this.prioridad = prioridad; }

    public Boolean getGeneraIndisponibilidad() { return generaIndisponibilidad; }
    public void setGeneraIndisponibilidad(Boolean generaIndisponibilidad) { this.generaIndisponibilidad = generaIndisponibilidad; }

    public LocalDateTime getFechaInicioIndisponibilidad() { return fechaInicioIndisponibilidad; }
    public void setFechaInicioIndisponibilidad(LocalDateTime f) { this.fechaInicioIndisponibilidad = f; }

    public LocalDateTime getFechaFinIndisponibilidad() { return fechaFinIndisponibilidad; }
    public void setFechaFinIndisponibilidad(LocalDateTime f) { this.fechaFinIndisponibilidad = f; }

    public BigDecimal getHorasIndisponibilidad() { return horasIndisponibilidad; }
    public void setHorasIndisponibilidad(BigDecimal horasIndisponibilidad) { this.horasIndisponibilidad = horasIndisponibilidad; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDateTime getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(LocalDateTime fechaCierre) { this.fechaCierre = fechaCierre; }

    public String getSolucion() { return solucion; }
    public void setSolucion(String solucion) { this.solucion = solucion; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Incidencia)) return false;
        Incidencia other = (Incidencia) o;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}