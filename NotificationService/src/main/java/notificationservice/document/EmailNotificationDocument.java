package notificationservice.document;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import notificationservice.enums.email.EmailNotificationStatus;
import notificationservice.enums.email.EmailNotificationType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Document(collection = "email_notifications")
public class EmailNotificationDocument {
    @Id
    private UUID id = UUID.randomUUID();

    @NotBlank
    @Email
    private String email;

    private UUID authUserId;

    @NotNull
    private EmailNotificationType type;
    private String verificationCode;

    @NotNull
    private EmailNotificationStatus status = EmailNotificationStatus.PENDING;

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
