package apigateway.dto.command.transaction;

import enums.auth.Roles;
import java.util.UUID;

public record GetTransactionByMeCommandDto(UUID authUserId, Roles role) {}
