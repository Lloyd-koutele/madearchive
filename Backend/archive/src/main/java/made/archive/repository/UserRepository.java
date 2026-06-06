package made.archive.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import made.archive.entite.Role;
import made.archive.entite.User;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID>
{
    Optional<User> findByEmail(String email);

    Optional<User> findByEmailAndRoles (String email, Role roles);
    
    boolean existsByEmail(String email);

    List<User> findByRoles(Role roles);

    List<User> findByActif(boolean actif);

    Optional<User> findById(Long userId);
}