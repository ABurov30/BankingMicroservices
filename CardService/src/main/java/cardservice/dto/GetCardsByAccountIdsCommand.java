package cardservice.dto;

import enums.auth.Roles;
import java.util.List;
import java.util.UUID;

public record GetCardsByAccountIdsCommand(List<UUID> accountIds, UUID authUserId, Roles role) {}
