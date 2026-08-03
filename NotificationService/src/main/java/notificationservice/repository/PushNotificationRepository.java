package notificationservice.repository;

import notificationservice.document.PushNotificationDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.UUID;

public interface PushNotificationRepository extends MongoRepository <PushNotificationDocument, UUID> {
    List<PushNotificationDocument> findByAuthUserIdOrderByCreatedAtDesc(UUID authUserId);

    List<PushNotificationDocument> findByAuthUserIdAndIdIn(UUID authUserId, List<UUID> ids);
}
