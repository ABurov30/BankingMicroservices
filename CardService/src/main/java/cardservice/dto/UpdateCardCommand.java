package cardservice.dto;

import enums.card.CardStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record UpdateCardCommand(
    UUID cardId,
    CardStatus status,
    BigDecimal dailyLimitMinorUnits,
    BigDecimal monthlyLimitMinorUnits,
    UUID authUserId,
    String role) {}
