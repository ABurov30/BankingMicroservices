package authservice.dto;

import java.util.UUID;

public record GetRoleByAuthUserIdCommand(UUID authUserId) {}
