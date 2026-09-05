package co.edu.uan.gestionhardware.repository;

import co.edu.uan.gestionhardware.model.Incidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IncidenciaRepository extends JpaRepository<Incidencia, Long> {

    @Query("""
           select i from Incidencia i
           join fetch i.equipo e
           join fetch e.area
           join fetch i.categoriaFalla
           join fetch i.reportadoPor
           order by i.fechaReporte desc
           """)
    List<Incidencia> findTodasConRelaciones();

    @Query("""
           select i from Incidencia i
           join fetch i.equipo e
           join fetch e.area
           join fetch i.categoriaFalla
           join fetch i.reportadoPor
           where i.estado <> 'CERRADA'
           order by i.fechaReporte desc
           """)
    List<Incidencia> findAbiertasConRelaciones();

    @Query("""
           select i from Incidencia i
           join fetch i.equipo
           join fetch i.categoriaFalla
           join fetch i.reportadoPor
           where i.equipo.id = :equipoId
           order by i.fechaReporte desc
           """)
    List<Incidencia> findPorEquipo(@Param("equipoId") Long equipoId);

    @Query("""
           select i from Incidencia i
           join fetch i.equipo e
           join fetch e.area
           join fetch i.categoriaFalla
           join fetch i.reportadoPor
           where i.id = :id
           """)
    Optional<Incidencia> findConRelaciones(@Param("id") Long id);

    long countByEquipoIdAndFechaReporteBetween(Long equipoId,
                                               LocalDateTime desde,
                                               LocalDateTime hasta);

    boolean existsByEquipoIdAndEstadoNot(Long equipoId, String estado);

  
  
    long countByEquipoId(Long equipoId);

    long countByEstadoNot(String estado);

    @Query("select coalesce(sum(i.horasIndisponibilidad), 0) from Incidencia i where i.equipo.id = :equipoId")
    java.math.BigDecimal sumHorasIndisponibilidadPorEquipo(@Param("equipoId") Long equipoId);

}