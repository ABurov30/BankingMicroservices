package notificationservice.repository;

import notificationservice.document.NotificationDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface NotificationRepository extends MongoRepository<NotificationDocument, UUID> {
}
