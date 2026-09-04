package co.edu.uan.gestionhardware.repository;

import co.edu.uan.gestionhardware.model.CategoriaFalla;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaFallaRepository extends JpaRepository<CategoriaFalla, Long> {

    Optional<CategoriaFalla> findByNombre(String nombre);

    List<CategoriaFalla> findAllByOrderByNombreAsc();
}