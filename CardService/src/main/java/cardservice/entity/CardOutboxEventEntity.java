package cardservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import outboxsupport.OutboxEventEntity;

@Entity
@Table(name = "card_outbox_events")
public class CardOutboxEventEntity extends OutboxEventEntity {}
