package authservice.dto;

import java.util.UUID;

public record VerifyAuthUserCommand(
        UUID authUserId,
        String verificationCode,
        String role
) {
}
