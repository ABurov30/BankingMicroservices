package notificationservice.repository;

import java.util.List;
import java.util.UUID;
import notificationservice.document.PushNotificationDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PushNotificationRepository
    extends MongoRepository<PushNotificationDocument, UUID> {
  List<PushNotificationDocument> findByAuthUserIdOrderByCreatedAtDesc(UUID authUserId);

  List<PushNotificationDocument> findByAuthUserIdAndIdIn(UUID authUserId, List<UUID> ids);
}
