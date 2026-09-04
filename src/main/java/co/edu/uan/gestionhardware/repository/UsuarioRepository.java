package co.edu.uan.gestionhardware.repository;

import co.edu.uan.gestionhardware.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @Query("""
           select u from Usuario u
           join fetch u.rol
           left join fetch u.area
           where u.email = :email
           """)
    Optional<Usuario> findByEmailConRol(@Param("email") String email);

    @Query("""
           select u from Usuario u
           join fetch u.rol
           left join fetch u.area
           where u.activo = true
           """)
    List<Usuario> findActivosConRol();

    @Query("""
       select u from Usuario u
       join fetch u.rol
       left join fetch u.area
       """)
       List<Usuario> findAllConRol();

    @Query("""
           select u from Usuario u
           join fetch u.rol
           left join fetch u.area
           where u.id = :id
           """)
           
    Optional<Usuario> findConRelaciones(@Param("id") Long id);

    boolean existsByEmail(String email);
}