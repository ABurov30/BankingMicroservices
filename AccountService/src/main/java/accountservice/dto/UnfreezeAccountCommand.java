package accountservice.dto;

import java.util.UUID;

public record UnfreezeAccountCommand(UUID accountId, UUID authUserId, String role) {}
