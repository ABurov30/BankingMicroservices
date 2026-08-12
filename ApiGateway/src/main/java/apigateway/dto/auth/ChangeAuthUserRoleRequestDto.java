package apigateway.dto.auth;

import enums.auth.Roles;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ChangeAuthUserRoleRequestDto(@NotNull UUID authUserId, @NotNull Roles role) {}
