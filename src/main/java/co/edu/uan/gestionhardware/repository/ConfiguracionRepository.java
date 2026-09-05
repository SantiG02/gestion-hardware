package co.edu.uan.gestionhardware.repository;

import co.edu.uan.gestionhardware.model.ConfiguracionSistema;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionRepository extends JpaRepository<ConfiguracionSistema, Long> {
}