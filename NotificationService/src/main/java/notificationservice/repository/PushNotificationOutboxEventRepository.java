package notificationservice.repository;

import java.util.List;
import java.util.UUID;
import notificationservice.entity.PushNotificationOutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import outboxsupport.OutboxEventStatus;

public interface PushNotificationOutboxEventRepository
    extends JpaRepository<PushNotificationOutboxEventEntity, UUID> {
  List<PushNotificationOutboxEventEntity> findTop50ByOutboxEventStatusOrderByCreatedAtAsc(
      OutboxEventStatus outboxEventStatus);
}
