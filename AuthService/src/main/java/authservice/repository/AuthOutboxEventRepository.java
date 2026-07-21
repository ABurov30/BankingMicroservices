package authservice.repository;

import authservice.entity.AuthOutboxEventEntity;
import outboxsupport.OutboxEventStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuthOutboxEventRepository extends JpaRepository<AuthOutboxEventEntity, UUID> {
    List<AuthOutboxEventEntity> findTop50ByOutboxEventStatusOrderByCreatedAtAsc(OutboxEventStatus outboxEventStatus);
}
