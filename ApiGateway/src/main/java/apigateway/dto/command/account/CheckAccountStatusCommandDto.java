package apigateway.dto.command.account;

import enums.auth.Roles;
import java.util.UUID;

public record CheckAccountStatusCommandDto(UUID accountId, UUID authUserId, Roles role) {}
