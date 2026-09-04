package apigateway.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record GetRecipientRequestDto(@Email @NotBlank String email) {}
