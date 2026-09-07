package apigateway.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record VerifyAuthUserByCodeRequestDto(
    @NotNull UUID authUserId, @NotBlank String verificationCode) {}
