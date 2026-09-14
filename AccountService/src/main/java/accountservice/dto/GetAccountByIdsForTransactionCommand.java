package accountservice.dto;

import java.util.List;
import java.util.UUID;

public record GetAccountByIdsForTransactionCommand(List<UUID> accountIds) {}
