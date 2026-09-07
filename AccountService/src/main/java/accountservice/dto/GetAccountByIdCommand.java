package accountservice.dto;

import enums.auth.Roles;
import java.util.UUID;

public record GetAccountByIdCommand(UUID accountId, UUID authUserId, Roles role) {}
