package userservice.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import outboxsupport.OutboxEventStatus;
import userservice.entity.UserOutboxEventEntity;

public interface UserOutboxEventRepository extends JpaRepository<UserOutboxEventEntity, UUID> {
  List<UserOutboxEventEntity> findTop50ByOutboxEventStatusOrderByCreatedAtAsc(
      OutboxEventStatus outboxEventStatus);
}
