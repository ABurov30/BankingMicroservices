package apigateway.dto.request.auth;

import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequestDto(
    @NotBlank String resetPasswordToken, @NotBlank String newPassword) {}
