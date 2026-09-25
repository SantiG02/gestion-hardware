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

        @Query("""
           select i from Incidencia i
           join fetch i.equipo e
           join fetch e.area
           where i.fechaReporte between :desde and :hasta
           order by i.fechaReporte
           """)
    java.util.List<Incidencia> findPorPeriodo(@Param("desde") java.time.LocalDateTime desde,
                                              @Param("hasta") java.time.LocalDateTime hasta);

           @Query("""
           select i from Incidencia i
           join fetch i.equipo e
           join fetch e.area
           join fetch i.categoriaFalla
           join fetch i.reportadoPor
           where i.reportadoPor.id = :usuarioId
           order by i.fechaReporte desc
           """)
    List<Incidencia> findPorUsuarioReportador(@Param("usuarioId") Long usuarioId);

    @Query("select count(i) from Incidencia i where i.reportadoPor.rol.nombre = 'USUARIO'")
    long countReportadasPorUsuarioFinal();

    @Query("select count(i) from Incidencia i where i.reportadoPor.rol.nombre = 'USUARIO' and i.estado = 'ABIERTA'")
    long countAbiertasPorUsuarioFinal();

    @Query("""
           select count(i) from Incidencia i
           where i.reportadoPor.rol.nombre = 'USUARIO'
           and i.estado = 'CERRADA' and i.confirmacionUsuario = 'PENDIENTE'
           """)
    long countPendientesConfirmacionUsuarioFinal();

    @Query("select count(i) from Incidencia i where i.reportadoPor.rol.nombre = 'USUARIO' and i.confirmacionUsuario = 'APROBADA'")
    long countConfirmadasUsuarioFinal();

    @Query("select coalesce(sum(i.vecesRechazada), 0) from Incidencia i where i.reportadoPor.rol.nombre = 'USUARIO'")
    long sumVecesRechazadaUsuarioFinal();

        @Query("""
           select i from Incidencia i
           join fetch i.equipo e
           join fetch e.area
           join fetch i.categoriaFalla c
           join fetch i.reportadoPor
           where (:texto is null
                  or lower(e.codigoInterno) like lower(concat('%', :texto, '%'))
                  or lower(e.marca) like lower(concat('%', :texto, '%'))
                  or lower(e.modelo) like lower(concat('%', :texto, '%')))
             and (:estado is null or i.estado = :estado)
             and (:prioridad is null or i.prioridad = :prioridad)
             and (:categoriaId is null or c.id = :categoriaId)
           order by i.fechaReporte desc
           """)
    List<Incidencia> filtrar(@Param("texto") String texto,
                             @Param("estado") String estado,
                             @Param("prioridad") String prioridad,
                             @Param("categoriaId") Long categoriaId);

}