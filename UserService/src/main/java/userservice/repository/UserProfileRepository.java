package userservice.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import userservice.entity.UserProfileEntity;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, UUID> {
  Optional<UserProfileEntity> findByAuthUserId(UUID authUserId);

  Optional<UserProfileEntity> findByEmail(String email);
}
