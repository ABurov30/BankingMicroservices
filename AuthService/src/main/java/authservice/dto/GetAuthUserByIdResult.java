package authservice.dto;

import enums.auth.AuthUserStatus;
import enums.auth.Roles;
import java.util.List;
import java.util.UUID;

public record GetAuthUserByIdResult(
    UUID authUserId,
    AuthUserStatus status,
    String email,
    Roles role,
    List<SocialAccountResult> socialAccounts) {}
