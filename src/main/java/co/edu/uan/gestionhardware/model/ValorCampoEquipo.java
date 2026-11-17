package co.edu.uan.gestionhardware.model;

import jakarta.persistence.*;

/**
 * Valor que un equipo concreto tiene para un campo propio de su tipo.
 */
@Entity
@Table(name = "valor_campo_equipo",
       uniqueConstraints = @UniqueConstraint(columnNames = {"equipo_id", "campo_id"}))
public class ValorCampoEquipo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipo_id", nullable = false)
    private Equipo equipo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campo_id", nullable = false)
    private CampoTipoEquipo campo;

    @Column(length = 255)
    private String valor;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Equipo getEquipo() { return equipo; }
    public void setEquipo(Equipo equipo) { this.equipo = equipo; }

    public CampoTipoEquipo getCampo() { return campo; }
    public void setCampo(CampoTipoEquipo campo) { this.campo = campo; }

    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }
}
