package authservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import outboxsupport.OutboxEventEntity;

@Entity
@Table(name = "auth_outbox_events")
public class AuthOutboxEventEntity extends OutboxEventEntity {}
