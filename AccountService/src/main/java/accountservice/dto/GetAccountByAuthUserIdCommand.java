package accountservice.dto;

import enums.auth.Roles;
import java.util.UUID;

public record GetAccountByAuthUserIdCommand(UUID authUserId, Roles role) {}
