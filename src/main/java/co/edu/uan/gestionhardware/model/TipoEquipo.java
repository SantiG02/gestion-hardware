package co.edu.uan.gestionhardware.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "tipo_equipo")
public class TipoEquipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre del tipo es obligatorio")
    @Size(max = 50, message = "Maximo 50 caracteres")
    @Column(nullable = false, length = 50, unique = true)
    private String nombre;

    @ColumnDefault("true")
    @Column(nullable = false)
    private Boolean activo = true;

    // Si es true, el formulario de equipos de este tipo muestra procesador,
    // RAM, almacenamiento y sistema operativo. Se desmarca para tipos como
    // impresoras o equipos de red, que usan sus propios campos.
    @ColumnDefault("true")
    @Column(name = "especificaciones_basicas", nullable = false)
    private Boolean especificacionesBasicas = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public Boolean getEspecificacionesBasicas() { return especificacionesBasicas; }
    public void setEspecificacionesBasicas(Boolean especificacionesBasicas) {
        this.especificacionesBasicas = especificacionesBasicas;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TipoEquipo)) return false;
        TipoEquipo other = (TipoEquipo) o;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
