package apigateway.dto.user;

import enums.auth.Roles;
import enums.user.UserProfileStatus;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record GetUserInfoResponseDto(
        UUID userProfileId,
        UUID autUserId,
        String email,
        String firstName,
        String lastName,
        UserProfileStatus status
) {
}
