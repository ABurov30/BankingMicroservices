package accountservice.dto;

import enums.auth.Roles;

public record GetAllAccountsCommand(Roles role) {}
