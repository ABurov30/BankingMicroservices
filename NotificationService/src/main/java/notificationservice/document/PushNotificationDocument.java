package notificationservice.document;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import notificationservice.enums.push.PushNotificationStatus;
import notificationservice.enums.push.PushNotificationType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Document(collection = "push_notifications")
public class PushNotificationDocument {
    @Id
    private UUID id = UUID.randomUUID();
    @NotNull
    private UUID authUserId;
    @NotNull
    private String title;
    @NotNull
    private String body;
    @NotNull
    private PushNotificationType type;
    @NotNull
    private PushNotificationStatus status;

    @NotNull
    private LocalDateTime createdAt = LocalDateTime.now();
}
