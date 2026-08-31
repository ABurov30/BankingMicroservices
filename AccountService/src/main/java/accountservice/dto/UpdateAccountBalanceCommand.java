package accountservice.dto;

import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record UpdateAccountBalanceCommand(
    UUID accountId, @Positive Long minorUnits, UUID authUserId) {}
