package co.edu.uan.gestionhardware.repository;

import co.edu.uan.gestionhardware.model.CampoTipoEquipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CampoTipoEquipoRepository extends JpaRepository<CampoTipoEquipo, Long> {

    List<CampoTipoEquipo> findByTipoEquipoIdOrderByIdAsc(Long tipoId);

    List<CampoTipoEquipo> findByTipoEquipoIdAndActivoTrueOrderByIdAsc(Long tipoId);

    boolean existsByTipoEquipoIdAndNombreIgnoreCase(Long tipoId, String nombre);

    boolean existsByTipoEquipoIdAndNombreIgnoreCaseAndIdNot(Long tipoId, String nombre, Long id);

    @Query("""
           select c from CampoTipoEquipo c
           join fetch c.tipoEquipo t
           where c.activo = true and t.activo = true
           order by t.id, c.id
           """)
    List<CampoTipoEquipo> findActivosDeTiposActivos();
}
