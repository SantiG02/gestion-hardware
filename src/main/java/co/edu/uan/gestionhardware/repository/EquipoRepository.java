package co.edu.uan.gestionhardware.repository;

import co.edu.uan.gestionhardware.model.Equipo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    @EntityGraph(attributePaths = {"area", "estadoEquipo", "tipoEquipo"})
    List<Equipo> findByActivoTrue();

    List<Equipo> findByAreaId(Long areaId);

    Optional<Equipo> findBySerial(String serial);
}