package apigateway.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequestDto(
    @NotBlank @Size(min = 8, max = 64) String oldPassword,
    @NotBlank @Size(min = 8, max = 64) String newPassword) {}
