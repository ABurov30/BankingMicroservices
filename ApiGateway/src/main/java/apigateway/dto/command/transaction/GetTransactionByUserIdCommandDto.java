package apigateway.dto.command.transaction;

import enums.auth.Roles;
import java.util.UUID;

public record GetTransactionByUserIdCommandDto(UUID ownerUserId, UUID authUserId, Roles role) {}
