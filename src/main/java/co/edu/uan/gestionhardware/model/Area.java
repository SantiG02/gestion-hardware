package co.edu.uan.gestionhardware.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "area",
       uniqueConstraints = @UniqueConstraint(columnNames = {"sede_id", "nombre"}))
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @NotBlank(message = "El nombre del area es obligatorio")
    @Size(max = 100, message = "Maximo 100 caracteres")
    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false)
    private Boolean activo = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Sede getSede() { return sede; }
    public void setSede(Sede sede) { this.sede = sede; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Area)) return false;
        Area other = (Area) o;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}