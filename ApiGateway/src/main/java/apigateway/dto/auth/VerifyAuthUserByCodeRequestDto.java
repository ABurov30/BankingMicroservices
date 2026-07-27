package apigateway.dto.auth;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record VerifyAuthUserByCodeRequestDto(
        @NotBlank
        UUID authUserId,
        @NotBlank
        String verificationCode
) {
}
