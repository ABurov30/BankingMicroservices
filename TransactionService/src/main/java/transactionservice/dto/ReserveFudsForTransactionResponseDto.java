package transactionservice.dto;

public record ReserveFudsForTransactionResponseDto(
    AccountResponseDto sourceAccount,
    AccountResponseDto targetAccount,
    ReservationResponseDto reservationResponse) {}
