package apigateway.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record VerifyAuthUserByCodeRequestDto(
        @NotNull
        UUID authUserId,
        @NotBlank
        String verificationCode
) {
}
