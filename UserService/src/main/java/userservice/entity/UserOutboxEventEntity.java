package userservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import outboxsupport.OutboxEventEntity;

@Entity
@Table(name = "user_outbox_events")
public class UserOutboxEventEntity extends OutboxEventEntity {}
