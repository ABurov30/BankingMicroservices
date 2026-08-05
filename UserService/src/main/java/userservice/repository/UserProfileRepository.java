package userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import userservice.entity.UserProfileEntity;

import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfileEntity, UUID> {
    Optional<UserProfileEntity> findByAuthUserId(UUID authUserId);

    Optional<UserProfileEntity> findByEmail (String email);
}
