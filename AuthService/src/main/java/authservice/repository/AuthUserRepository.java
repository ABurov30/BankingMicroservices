package authservice.repository;

import authservice.entity.AuthUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuthUserRepository extends JpaRepository<AuthUserEntity, UUID> {
    boolean existsByEmail(String email);
    Optional<AuthUserEntity> findByEmail (String email);
}
