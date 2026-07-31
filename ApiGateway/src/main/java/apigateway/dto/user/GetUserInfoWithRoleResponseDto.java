package apigateway.dto.user;

import enums.auth.Roles;

public record GetUserInfoWithRoleResponseDto(
        GetUserInfoResponseDto userInfo,
        GetRoleByAuthUserIdResponseDto role
) {
}
