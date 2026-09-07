package cardservice.dto;

import enums.auth.Roles;
import java.util.UUID;

public record GetCardsByAccountIdCommand(UUID accountId, UUID authUserId, Roles role) {}
