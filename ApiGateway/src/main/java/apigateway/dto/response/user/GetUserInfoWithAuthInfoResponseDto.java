package apigateway.dto.response.user;

import apigateway.dto.response.auth.SocialAccountResponse;
import enums.auth.AuthUserStatus;
import enums.auth.Roles;
import java.util.List;

public record GetUserInfoWithAuthInfoResponseDto(
    GetUserInfoResponseDto userInfo,
    Roles role,
    AuthUserStatus status,
    List<SocialAccountResponse> socialAccounts) {}
