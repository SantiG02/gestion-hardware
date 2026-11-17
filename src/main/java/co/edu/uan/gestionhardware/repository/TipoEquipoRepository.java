package co.edu.uan.gestionhardware.repository;

import co.edu.uan.gestionhardware.model.TipoEquipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TipoEquipoRepository extends JpaRepository<TipoEquipo, Long> {

    List<TipoEquipo> findAllByOrderByNombreAsc();

    List<TipoEquipo> findByActivoTrueOrderByNombreAsc();

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);
}