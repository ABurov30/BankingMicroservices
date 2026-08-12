package authservice.repository;

import authservice.entity.AuthUserEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthUserRepository extends JpaRepository<AuthUserEntity, UUID> {
  boolean existsByEmail(String email);

  Optional<AuthUserEntity> findByEmail(String email);
}
