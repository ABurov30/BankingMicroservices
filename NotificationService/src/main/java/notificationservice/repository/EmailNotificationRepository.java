package notificationservice.repository;

import java.util.UUID;
import notificationservice.document.EmailNotificationDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface EmailNotificationRepository
    extends MongoRepository<EmailNotificationDocument, UUID> {}
