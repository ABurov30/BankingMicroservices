package apigateway.dto.request.account;

import enums.auth.Roles;
import java.util.UUID;

public record GetAccountsByAuthUserIdRequestDto(UUID authUserId, Roles role) {}
