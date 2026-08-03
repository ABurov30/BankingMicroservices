package authservice.dto;

import enums.auth.AuthUserStatus;
import enums.auth.Roles;

import java.util.UUID;

public record GetAuthUserByIdResult(
        UUID authUserId,
        AuthUserStatus status,
        String email,
        Roles role
) {
}
