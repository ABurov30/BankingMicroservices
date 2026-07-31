package authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgetPasswordCommand(
        @NotBlank
        @Email
        String email
) {
}
