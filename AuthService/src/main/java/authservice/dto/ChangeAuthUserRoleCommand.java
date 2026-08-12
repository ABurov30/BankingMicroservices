package authservice.dto;

import enums.auth.Roles;
import java.util.UUID;

public record ChangeAuthUserRoleCommand(UUID authUserId, Roles role) {}
