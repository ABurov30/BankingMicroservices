package cardservice.dto;

import java.util.UUID;

public record CompensateLimitsForTransactionCommand(UUID transactionId) {}
