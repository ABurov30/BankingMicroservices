package transactionservice.dto;

import enums.account.ReservationStatus;

public record ReservationResponseDto(ReservationStatus status, String message) {}
