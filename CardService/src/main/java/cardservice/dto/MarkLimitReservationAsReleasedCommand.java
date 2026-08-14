package cardservice.dto;

import java.util.UUID;

public record MarkLimitReservationAsReleasedCommand(UUID transactionId) {}
