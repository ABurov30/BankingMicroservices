package transactionservice.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import outboxsupport.OutboxEventStatus;
import transactionservice.entity.TransactionOutboxEventEntity;

public interface TransactionOutboxEventRepository
    extends JpaRepository<TransactionOutboxEventEntity, UUID> {
  List<TransactionOutboxEventEntity> findTop50ByOutboxEventStatusOrderByCreatedAtAsc(
      OutboxEventStatus outboxEventStatus);
}
