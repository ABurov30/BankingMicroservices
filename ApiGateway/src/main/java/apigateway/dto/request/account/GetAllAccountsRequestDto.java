package apigateway.dto.request.account;

import enums.auth.Roles;

public record GetAllAccountsRequestDto(Roles role) {}
