package apigateway.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record GetUserInfoByEmailRequestDto(
        @Email
        @NotBlank
        String email
) {
}
