package co.edu.uan.gestionhardware.repository;

import co.edu.uan.gestionhardware.model.ValorCampoEquipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ValorCampoEquipoRepository extends JpaRepository<ValorCampoEquipo, Long> {

    @Query("""
           select v from ValorCampoEquipo v
           join fetch v.campo c
           where v.equipo.id = :equipoId and c.activo = true
           order by c.id
           """)
    List<ValorCampoEquipo> findPorEquipo(@Param("equipoId") Long equipoId);

    @Modifying(flushAutomatically = true)
    @Query("delete from ValorCampoEquipo v where v.equipo.id = :equipoId")
    void eliminarPorEquipo(@Param("equipoId") Long equipoId);
}
