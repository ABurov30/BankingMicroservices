package transactionservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import outboxsupport.OutboxEventEntity;

@Entity
@Table(name = "transaction_outbox_events")
public class TransactionOutboxEventEntity extends OutboxEventEntity {
}
