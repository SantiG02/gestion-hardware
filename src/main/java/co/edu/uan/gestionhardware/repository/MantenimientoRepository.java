package co.edu.uan.gestionhardware.repository;

import co.edu.uan.gestionhardware.model.Mantenimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MantenimientoRepository extends JpaRepository<Mantenimiento, Long> {

    @Query("""
           select m from Mantenimiento m
           join fetch m.equipo e
           join fetch e.area
           join fetch m.tecnico
           left join fetch m.incidencia
           order by m.fechaEjecucion desc, m.fechaProgramada desc
           """)
    List<Mantenimiento> findTodosConRelaciones();

    @Query("""
           select m from Mantenimiento m
           join fetch m.equipo e
           join fetch e.area
           join fetch m.tecnico
           left join fetch m.incidencia
           where m.estado <> 'FINALIZADO'
           order by m.fechaProgramada
           """)
    List<Mantenimiento> findPendientesConRelaciones();

    @Query("""
           select m from Mantenimiento m
           join fetch m.equipo
           join fetch m.tecnico
           left join fetch m.incidencia
           where m.equipo.id = :equipoId
           order by m.fechaEjecucion desc, m.fechaProgramada desc
           """)
    List<Mantenimiento> findPorEquipo(@Param("equipoId") Long equipoId);

    @Query("""
           select m from Mantenimiento m
           join fetch m.equipo e
           join fetch e.area
           join fetch m.tecnico
           left join fetch m.incidencia
           where m.id = :id
           """)
    Optional<Mantenimiento> findConRelaciones(@Param("id") Long id);

    long countByTipoAndFechaEjecucionBetween(String tipo, LocalDate desde, LocalDate hasta);

    long countByEquipoIdAndTipo(Long equipoId, String tipo);

    @Query("""
           select m from Mantenimiento m
           join fetch m.equipo
           join fetch m.tecnico
           where m.estado = 'PROGRAMADO'
             and m.fechaProgramada <= :limite
           order by m.fechaProgramada
           """)
    List<Mantenimiento> findProximosAVencer(@Param("limite") LocalDate limite);

   long countByEstadoNot(String estado);

       @Query("""
           select m from Mantenimiento m
           join fetch m.equipo e
           join fetch e.area
           where m.fechaEjecucion between :desde and :hasta
           order by m.fechaEjecucion
           """)
    List<Mantenimiento> findEjecutadosPorPeriodo(@Param("desde") java.time.LocalDate desde,
                                                 @Param("hasta") java.time.LocalDate hasta);
       
           @Query("""
           select m from Mantenimiento m
           join fetch m.equipo e
           join fetch e.area
           join fetch m.tecnico t
           left join fetch m.incidencia
           where (:texto is null
                  or lower(e.codigoInterno) like lower(concat('%', :texto, '%'))
                  or lower(e.marca) like lower(concat('%', :texto, '%'))
                  or lower(e.modelo) like lower(concat('%', :texto, '%')))
             and (:tipo is null or m.tipo = :tipo)
             and (:estado is null or m.estado = :estado)
             and (:tecnicoId is null or t.id = :tecnicoId)
           order by m.fechaEjecucion desc, m.fechaProgramada desc
           """)
    List<Mantenimiento> filtrar(@Param("texto") String texto,
                                @Param("tipo") String tipo,
                                @Param("estado") String estado,
                                @Param("tecnicoId") Long tecnicoId);

}