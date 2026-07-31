package authservice.dto;

import enums.auth.Roles;

public record GetRoleByAuthUserIdResult(
        Roles role
) {
}
