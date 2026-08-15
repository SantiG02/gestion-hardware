package co.edu.uan.gestionhardware.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Entity
@Table(name = "equipo")
public class Equipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El codigo interno es obligatorio")
    @Size(max = 30, message = "Maximo 30 caracteres")
    @Column(name = "codigo_interno", nullable = false, length = 30, unique = true)
    private String codigoInterno;

    @NotBlank(message = "El serial es obligatorio")
    @Size(max = 80, message = "Maximo 80 caracteres")
    @Column(nullable = false, length = 80, unique = true)
    private String serial;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_equipo_id")
    private TipoEquipo tipoEquipo;

    @NotNull(message = "Debe seleccionar un area")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "area_id", nullable = false)
    private Area area;

    @NotNull(message = "Debe seleccionar un estado")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "estado_equipo_id", nullable = false)
    private EstadoEquipo estadoEquipo;

    @NotBlank(message = "La marca es obligatoria")
    @Size(max = 60, message = "Maximo 60 caracteres")
    @Column(nullable = false, length = 60)
    private String marca;

    @NotBlank(message = "El modelo es obligatorio")
    @Size(max = 80, message = "Maximo 80 caracteres")
    @Column(nullable = false, length = 80)
    private String modelo;

    @Size(max = 100, message = "Maximo 100 caracteres")
    @Column(length = 100)
    private String procesador;

    @Min(value = 1, message = "La RAM debe ser mayor a cero")
    @Column(name = "ram_gb")
    private Integer ramGb;

    @Column(name = "tipo_almacenamiento", length = 20)
    private String tipoAlmacenamiento;

    @Min(value = 1, message = "La capacidad debe ser mayor a cero")
    @Column(name = "capacidad_gb")
    private Integer capacidadGb;

    @Size(max = 80, message = "Maximo 80 caracteres")
    @Column(name = "sistema_operativo", length = 80)
    private String sistemaOperativo;

    @PastOrPresent(message = "La fecha no puede ser futura")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "fecha_ultima_actualizacion_so")
    private LocalDate fechaUltimaActualizacionSo;

    @NotNull(message = "La fecha de compra es obligatoria")
    @PastOrPresent(message = "La fecha no puede ser futura")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "fecha_compra", nullable = false)
    private LocalDate fechaCompra;

    @PastOrPresent(message = "La fecha no puede ser futura")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(name = "fecha_puesta_operacion")
    private LocalDate fechaPuestaOperacion;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(nullable = false)
    private Boolean activo = true;

    // Getters y setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigoInterno() { return codigoInterno; }
    public void setCodigoInterno(String codigoInterno) { this.codigoInterno = codigoInterno; }

    public String getSerial() { return serial; }
    public void setSerial(String serial) { this.serial = serial; }

    public TipoEquipo getTipoEquipo() { return tipoEquipo; }
    public void setTipoEquipo(TipoEquipo tipoEquipo) { this.tipoEquipo = tipoEquipo; }

    public Area getArea() { return area; }
    public void setArea(Area area) { this.area = area; }

    public EstadoEquipo getEstadoEquipo() { return estadoEquipo; }
    public void setEstadoEquipo(EstadoEquipo estadoEquipo) { this.estadoEquipo = estadoEquipo; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public String getProcesador() { return procesador; }
    public void setProcesador(String procesador) { this.procesador = procesador; }

    public Integer getRamGb() { return ramGb; }
    public void setRamGb(Integer ramGb) { this.ramGb = ramGb; }

    public String getTipoAlmacenamiento() { return tipoAlmacenamiento; }
    public void setTipoAlmacenamiento(String tipoAlmacenamiento) { this.tipoAlmacenamiento = tipoAlmacenamiento; }

    public Integer getCapacidadGb() { return capacidadGb; }
    public void setCapacidadGb(Integer capacidadGb) { this.capacidadGb = capacidadGb; }

    public String getSistemaOperativo() { return sistemaOperativo; }
    public void setSistemaOperativo(String sistemaOperativo) { this.sistemaOperativo = sistemaOperativo; }

    public LocalDate getFechaUltimaActualizacionSo() { return fechaUltimaActualizacionSo; }
    public void setFechaUltimaActualizacionSo(LocalDate f) { this.fechaUltimaActualizacionSo = f; }

    public LocalDate getFechaCompra() { return fechaCompra; }
    public void setFechaCompra(LocalDate fechaCompra) { this.fechaCompra = fechaCompra; }

    public LocalDate getFechaPuestaOperacion() { return fechaPuestaOperacion; }
    public void setFechaPuestaOperacion(LocalDate f) { this.fechaPuestaOperacion = f; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}