package co.edu.uan.gestionhardware.repository;

import co.edu.uan.gestionhardware.model.Equipo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    @EntityGraph(attributePaths = {"area", "estadoEquipo", "tipoEquipo"})
    List<Equipo> findByActivoTrue();

    @Query("""
           select e from Equipo e
           left join fetch e.area
           left join fetch e.estadoEquipo
           left join fetch e.tipoEquipo
           where e.id = :id
           """)
    Optional<Equipo> findConRelaciones(@Param("id") Long id);

    List<Equipo> findByAreaId(Long areaId);

    Optional<Equipo> findBySerial(String serial);
}