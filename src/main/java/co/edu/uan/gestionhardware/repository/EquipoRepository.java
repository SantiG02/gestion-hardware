package co.edu.uan.gestionhardware.repository;

import co.edu.uan.gestionhardware.model.Equipo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EquipoRepository extends JpaRepository<Equipo, Long> {

    @EntityGraph(attributePaths = {"area", "estadoEquipo", "tipoEquipo", "usuarioAsignado"})
    List<Equipo> findByActivoTrue();

    @Query("""
           select e from Equipo e
           left join fetch e.area
           left join fetch e.estadoEquipo
           left join fetch e.tipoEquipo
           left join fetch e.usuarioAsignado
           where e.id = :id
           """)
    Optional<Equipo> findConRelaciones(@Param("id") Long id);

    List<Equipo> findByAreaId(Long areaId);

    Optional<Equipo> findBySerial(String serial);

    long countByAreaIdAndActivoTrue(Long areaId);

    boolean existsByCodigoInterno(String codigoInterno);

    boolean existsBySerial(String serial);

        @Query("""
           select e from Equipo e
           left join fetch e.area a
           left join fetch a.sede s
           left join fetch e.estadoEquipo
           left join fetch e.tipoEquipo
           left join fetch e.usuarioAsignado
           where e.activo = true
             and (:texto is null
                  or lower(e.codigoInterno) like lower(concat('%', :texto, '%'))
                  or lower(e.serial) like lower(concat('%', :texto, '%'))
                  or lower(e.marca) like lower(concat('%', :texto, '%'))
                  or lower(e.modelo) like lower(concat('%', :texto, '%')))
             and (:sedeId is null or s.id = :sedeId)
             and (:areaId is null or a.id = :areaId)
             and (:estadoId is null or e.estadoEquipo.id = :estadoId)
             and (:responsableId is null or e.usuarioAsignado.id = :responsableId)
           order by e.codigoInterno
           """)
    List<Equipo> filtrar(@Param("texto") String texto,
                         @Param("sedeId") Long sedeId,
                         @Param("areaId") Long areaId,
                         @Param("estadoId") Long estadoId,
                         @Param("responsableId") Long responsableId);
}