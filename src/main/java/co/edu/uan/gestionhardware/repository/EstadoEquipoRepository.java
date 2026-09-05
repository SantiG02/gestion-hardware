package co.edu.uan.gestionhardware.repository;

import co.edu.uan.gestionhardware.model.EstadoEquipo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EstadoEquipoRepository extends JpaRepository<EstadoEquipo, Long> {
    List<EstadoEquipo> findAllByOrderByOrdenAsc();
    Optional<EstadoEquipo> findByNombre(String nombre);
}