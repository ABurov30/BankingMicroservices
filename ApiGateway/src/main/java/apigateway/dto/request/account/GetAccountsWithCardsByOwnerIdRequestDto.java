package apigateway.dto.request.account;

import enums.auth.Roles;
import java.util.UUID;

public record GetAccountsWithCardsByOwnerIdRequestDto(
    UUID ownerUserId, UUID authUserId, Roles role) {}
