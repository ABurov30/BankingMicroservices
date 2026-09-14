package apigateway.dto.command.card;

import enums.auth.Roles;
import java.util.List;
import java.util.UUID;

public record GetCardsByAccountIdsCommandDto(List<UUID> accountIds, UUID authUserId, Roles role) {}
