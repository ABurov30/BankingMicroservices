package apigateway.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequestDto(
    @Email @NotBlank String email,
    @NotBlank @Size(min = 8, max = 64) String password,
    @NotBlank String firstName,
    @NotBlank String lastName) {}
