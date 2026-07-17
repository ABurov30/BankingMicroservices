package apigateway.dto.user;

import apigateway.enums.UserProfileStatus;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record GetUserInfoResponseDto(
        UUID userProfileId,
        String email,
        String firstName,
        String lastName,
        UserProfileStatus status
) {
}
