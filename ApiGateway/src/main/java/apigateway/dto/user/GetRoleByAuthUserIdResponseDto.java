package apigateway.dto.user;

import enums.auth.Roles;

public record GetRoleByAuthUserIdResponseDto(
        Roles role
) {
}
