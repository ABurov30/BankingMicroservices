package transactionservice.dto;

import enums.account.ReservationStatus;

public record ReserveFudsForTransactionResponseDto(
        AccountResponseDto sourceAccount,
        AccountResponseDto targetAccount,
        ReservationStatus status,
        String message
) {
}
