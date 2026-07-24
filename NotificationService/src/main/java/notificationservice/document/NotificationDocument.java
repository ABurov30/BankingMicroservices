package notificationservice.document;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import notificationservice.enums.NotificationStatus;
import notificationservice.enums.NotificationType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Document(collection = "notifications")
public class NotificationDocument {
    @Id
    private UUID id = UUID.randomUUID();

    @NotBlank
    @Email
    private String email;

    @NotNull
    private NotificationType type;
    private String verificationCode;

    @NotNull
    private NotificationStatus status = NotificationStatus.PENDING;

    @NotNull
    private LocalDateTime createdAt = LocalDateTime.now();
    @NotNull
    private LocalDateTime nextRetryAt = LocalDateTime.now();
    private LocalDateTime sentAt;
    private LocalDateTime failedAt;

    private String errorMessage;
    private int retryCount = 0;
    private int maxRetryCount = 3;
}
