package cardservice.dto;

import enums.card.CardStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateCardCommand(
        UUID cardId,
        CardStatus status,
        BigDecimal dailyLimit,
        BigDecimal monthlyLimit
) {
}
