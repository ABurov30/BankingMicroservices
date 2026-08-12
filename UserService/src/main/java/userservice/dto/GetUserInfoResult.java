package userservice.dto;

import enums.user.UserProfileStatus;
import java.util.UUID;

public record GetUserInfoResult(
    UUID userProfileId,
    UUID authUserId,
    String email,
    String firstName,
    String lastName,
    UserProfileStatus status) {}
