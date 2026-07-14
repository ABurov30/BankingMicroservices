package apigateway.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequestDto(
        @Email
        @NotBlank
        String email,

        @NotBlank
        @Size(min = 8, max = 64)
        String password
) {
}
