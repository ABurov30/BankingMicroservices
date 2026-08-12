package accountservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateAccountBalanceCommand(UUID accountId, BigDecimal amount, UUID authUserId) {}
