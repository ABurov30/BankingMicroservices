package apigateway.dto.auth;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.UUID;

public record LogoutRequestDto(
        @NotBlank
        String refreshToken
) {
}
