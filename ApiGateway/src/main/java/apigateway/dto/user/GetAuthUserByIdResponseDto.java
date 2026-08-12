package apigateway.dto.user;

import enums.auth.AuthUserStatus;
import enums.auth.Roles;
import java.util.UUID;

public record GetAuthUserByIdResponseDto(
    UUID authUserId, String email, Roles role, AuthUserStatus status) {}
