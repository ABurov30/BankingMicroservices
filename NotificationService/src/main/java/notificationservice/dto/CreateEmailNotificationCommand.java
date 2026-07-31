package notificationservice.dto;

import notificationservice.enums.NotificationType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateEmailNotificationCommand(
        UUID authUserId,
        @NotBlank
        @Email
        String email,
        @NotNull
        NotificationType type,
        String verificationCode
) {
}
