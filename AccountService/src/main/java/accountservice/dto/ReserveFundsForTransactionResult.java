package accountservice.dto;

import enums.account.ReservationStatus;

public record ReserveFundsForTransactionResult(
        GetAccountResult sourceAccount,
        GetAccountResult targetAccount,
        ReservationStatus status,
        String message
) {
}
