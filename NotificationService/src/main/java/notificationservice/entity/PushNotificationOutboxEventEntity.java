package notificationservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import outboxsupport.OutboxEventEntity;

@Entity
@Table(name = "push_notification_outbox_events")
public class PushNotificationOutboxEventEntity extends OutboxEventEntity {}
