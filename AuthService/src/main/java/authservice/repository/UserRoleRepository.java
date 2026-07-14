package authservice.repository;

import authservice.entity.UserRoleEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UUID> {
     @EntityGraph(attributePaths = "role")
     Optional<UserRoleEntity> findByAuthUserId(UUID userId);
}
