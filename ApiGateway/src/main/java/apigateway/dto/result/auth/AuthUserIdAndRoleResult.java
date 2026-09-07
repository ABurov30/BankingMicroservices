package apigateway.dto.result.auth;

import enums.auth.Roles;
import java.util.UUID;

public record AuthUserIdAndRoleResult(UUID authUserId, Roles role) {}
