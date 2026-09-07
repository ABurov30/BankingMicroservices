package apigateway.dto.command.card;

import enums.auth.Roles;
import java.util.UUID;

public record GetCardsByAccountIdCommandDto(UUID accountId, UUID authUserId, Roles role) {}
