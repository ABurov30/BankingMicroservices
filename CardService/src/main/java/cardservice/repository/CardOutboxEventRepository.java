package cardservice.repository;

import cardservice.entity.CardOutboxEventEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import outboxsupport.OutboxEventStatus;

public interface CardOutboxEventRepository extends JpaRepository<CardOutboxEventEntity, UUID> {
  List<CardOutboxEventEntity> findTop50ByOutboxEventStatusOrderByCreatedAtAsc(
      OutboxEventStatus outboxEventStatus);
}
