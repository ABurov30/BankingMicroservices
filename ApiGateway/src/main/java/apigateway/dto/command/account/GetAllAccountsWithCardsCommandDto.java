package apigateway.dto.command.account;

import enums.auth.Roles;
import java.util.UUID;

public record GetAllAccountsWithCardsCommandDto(UUID authUserId, Roles role) {}
