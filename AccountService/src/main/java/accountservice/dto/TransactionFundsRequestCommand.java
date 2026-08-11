package accountservice.dto;

import java.util.UUID;

public record TransactionFundsRequestCommand(
        UUID transactionId,
        UUID targetAccountId,
        UUID targetAuthUserId
) {
}
