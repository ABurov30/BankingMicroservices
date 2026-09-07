package apigateway.dto.request.account;

import enums.auth.Roles;
import java.util.UUID;

public record GetAccountByIdRequestDto(UUID accountId, UUID authUserId, Roles role) {}
