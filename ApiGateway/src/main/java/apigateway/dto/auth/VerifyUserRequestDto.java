package apigateway.dto.auth;

import java.util.UUID;

public record VerifyUserRequestDto(
        UUID authUserId,
        String verificationCode,
        String role
) {
}
