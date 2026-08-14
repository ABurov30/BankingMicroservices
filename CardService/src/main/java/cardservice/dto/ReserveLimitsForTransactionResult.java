package cardservice.dto;

import enums.account.ReservationStatus;

public record ReserveLimitsForTransactionResult(ReservationStatus status, String message) {}
