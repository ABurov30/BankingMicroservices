package apigateway.dto.command.account;

import enums.auth.Roles;
import java.util.UUID;

public record GetAccountsWithCardsByOwnerIdCommandDto(
    UUID ownerUserId, UUID authUserId, Roles role) {}
