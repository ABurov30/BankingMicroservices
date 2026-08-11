package accountservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CompensationFundsCommand(
        UUID accountHoldId
) {
}
