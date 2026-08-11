package transactionservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import outboxsupport.OutboxEventStatus;
import transactionservice.entity.TransactionOutboxEventEntity;

import java.util.List;
import java.util.UUID;

public interface TransactionOutboxEventRepository extends JpaRepository<TransactionOutboxEventEntity, UUID> {
    List<TransactionOutboxEventEntity> findTop50ByOutboxEventStatusOrderByCreatedAtAsc(OutboxEventStatus outboxEventStatus);
}
