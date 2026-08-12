package authservice.repository;

import authservice.entity.AuthOutboxEventEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import outboxsupport.OutboxEventStatus;

public interface AuthOutboxEventRepository extends JpaRepository<AuthOutboxEventEntity, UUID> {
  List<AuthOutboxEventEntity> findTop50ByOutboxEventStatusOrderByCreatedAtAsc(
      OutboxEventStatus outboxEventStatus);
}
