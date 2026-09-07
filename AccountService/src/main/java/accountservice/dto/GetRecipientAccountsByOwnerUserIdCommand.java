package accountservice.dto;

import java.util.UUID;

public record GetRecipientAccountsByOwnerUserIdCommand(UUID ownerUserId) {}
