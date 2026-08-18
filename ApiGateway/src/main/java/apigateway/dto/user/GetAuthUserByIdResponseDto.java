package apigateway.dto.user;

import apigateway.dto.auth.SocialAccountResponse;
import enums.auth.AuthUserStatus;
import enums.auth.Roles;
import java.util.List;
import java.util.UUID;

public record GetAuthUserByIdResponseDto(
    UUID authUserId,
    String email,
    Roles role,
    AuthUserStatus status,
    List<SocialAccountResponse> socialAccounts) {}
