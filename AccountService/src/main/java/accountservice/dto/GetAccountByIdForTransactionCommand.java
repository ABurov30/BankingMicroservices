package accountservice.dto;

import java.util.UUID;

public record GetAccountByIdForTransactionCommand(UUID accountId) {}
