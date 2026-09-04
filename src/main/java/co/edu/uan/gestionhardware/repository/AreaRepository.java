package co.edu.uan.gestionhardware.repository;

import co.edu.uan.gestionhardware.model.Area;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AreaRepository extends JpaRepository<Area, Long> {

    List<Area> findBySedeId(Long sedeId);

    long countBySedeIdAndActivoTrue(Long sedeId);

    @Query("""
           select a from Area a
           join fetch a.sede
           where a.sede.id = :sedeId
           order by a.nombre
           """)
    List<Area> findConSedePorSede(@Param("sedeId") Long sedeId);

    @Query("""
           select a from Area a
           join fetch a.sede
           where a.id = :id
           """)
    Optional<Area> findConSede(@Param("id") Long id);

    @Query("""
           select a from Area a
           join fetch a.sede s
           where a.activo = true and s.activo = true
           order by s.nombre, a.nombre
           """)
    List<Area> findActivasConSede();

    boolean existsBySedeIdAndNombreIgnoreCase(Long sedeId, String nombre);
}