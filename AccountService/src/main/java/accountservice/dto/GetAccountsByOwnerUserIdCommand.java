package accountservice.dto;

import enums.auth.Roles;
import java.util.UUID;

public record GetAccountsByOwnerUserIdCommand(UUID ownerUserId, UUID authUserId, Roles role) {}
