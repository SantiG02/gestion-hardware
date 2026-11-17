package co.edu.uan.gestionhardware.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Definicion de un campo propio de un tipo de equipo (por ejemplo
 * "Velocidad (ppm)" para impresoras). El gestor la crea; el formulario de
 * equipos la muestra cuando se elige ese tipo.
 */
@Entity
@Table(name = "campo_tipo_equipo",
       uniqueConstraints = @UniqueConstraint(columnNames = {"tipo_equipo_id", "nombre"}))
public class CampoTipoEquipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tipo_equipo_id", nullable = false)
    private TipoEquipo tipoEquipo;

    @NotBlank(message = "El nombre del campo es obligatorio")
    @Size(max = 60, message = "Maximo 60 caracteres")
    @Column(nullable = false, length = 60)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_dato", nullable = false, length = 20)
    private TipoDatoCampo tipoDato = TipoDatoCampo.TEXTO;

    // Solo para tipoDato = LISTA: opciones separadas por coma.
    @Size(max = 500, message = "Maximo 500 caracteres")
    @Column(length = 500)
    private String opciones;

    @Column(nullable = false)
    private Boolean obligatorio = false;

    @Column(nullable = false)
    private Boolean activo = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public TipoEquipo getTipoEquipo() { return tipoEquipo; }
    public void setTipoEquipo(TipoEquipo tipoEquipo) { this.tipoEquipo = tipoEquipo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public TipoDatoCampo getTipoDato() { return tipoDato; }
    public void setTipoDato(TipoDatoCampo tipoDato) { this.tipoDato = tipoDato; }

    public String getOpciones() { return opciones; }
    public void setOpciones(String opciones) { this.opciones = opciones; }

    public Boolean getObligatorio() { return obligatorio; }
    public void setObligatorio(Boolean obligatorio) { this.obligatorio = obligatorio; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CampoTipoEquipo)) return false;
        CampoTipoEquipo other = (CampoTipoEquipo) o;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
