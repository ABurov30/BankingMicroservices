package authservice.repository;

import authservice.entity.UserRoleEntity;
import enums.auth.Roles;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRoleEntity, UUID> {
  boolean existsByRoleName(Roles name);

  @EntityGraph(attributePaths = "role")
  Optional<UserRoleEntity> findByAuthUserId(UUID userId);

  @EntityGraph(attributePaths = "role")
  List<UserRoleEntity> findByAuthUserIdIn(List<UUID> authUserIds);
}
