package apigateway.dto.user;

import enums.user.UserProfileStatus;
import java.util.UUID;

public record GetUserInfoResponseDto(
    UUID userProfileId,
    UUID autUserId,
    String email,
    String firstName,
    String lastName,
    UserProfileStatus status) {}
