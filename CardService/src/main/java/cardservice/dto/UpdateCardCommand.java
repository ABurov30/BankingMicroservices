package cardservice.dto;

import enums.card.CardStatus;
import java.util.UUID;

public record UpdateCardCommand(
    UUID cardId,
    CardStatus status,
    Long dailyLimitMinorUnits,
    Long monthlyLimitMinorUnits,
    UUID authUserId,
    String role) {}
