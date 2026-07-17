package userservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import userservice.entity.UserOutboxEventEntity;

import java.util.UUID;

public interface UserOutboxEventRepository extends JpaRepository<UserOutboxEventEntity, UUID> {
}
