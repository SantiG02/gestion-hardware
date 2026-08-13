package co.edu.uan.gestionhardware.repository;

import co.edu.uan.gestionhardware.model.Area;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AreaRepository extends JpaRepository<Area, Long> {
    List<Area> findBySedeId(Long sedeId);
}