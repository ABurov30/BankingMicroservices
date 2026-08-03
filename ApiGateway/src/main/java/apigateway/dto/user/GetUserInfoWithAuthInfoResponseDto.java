package apigateway.dto.user;

import enums.auth.AuthUserStatus;
import enums.auth.Roles;

public record GetUserInfoWithAuthInfoResponseDto(
        GetUserInfoResponseDto userInfo,
        Roles role,
        AuthUserStatus status
) {
}
