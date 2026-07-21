package accountservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import outboxsupport.OutboxEventEntity;

@Entity
@Table(name = "account_outbox_events")
public class AccountOutboxEventEntity extends OutboxEventEntity {
}
