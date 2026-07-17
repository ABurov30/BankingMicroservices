package userservice.dto;

import userservice.enums.UserProfileStatus;

import java.util.UUID;

public record GetUserInfoResult(
        UUID userProfileId,
        String email,
        String firstName,
        String lastName,
        UserProfileStatus status
) {
}
