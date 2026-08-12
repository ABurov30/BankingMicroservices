package accountservice.dto;

import java.util.UUID;

public record GetAccountsByOwnerUserIdCommand(UUID ownerUserId) {}
