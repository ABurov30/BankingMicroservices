package accountservice.dto;

import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

public record UpdateAccountBalanceCommand(
    UUID accountId, @Positive BigDecimal minorUnits, UUID authUserId) {}
