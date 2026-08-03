package notificationservice.repository;

import notificationservice.document.EmailNotificationDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

public interface EmailNotificationRepository extends MongoRepository<EmailNotificationDocument, UUID> {
}
