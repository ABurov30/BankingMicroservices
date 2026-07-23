package cardservice.dto;

import enums.card.CardStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateCardResult(
        UUID cardId,
        UUID accountId,
        String pan,
        CardStatus status,
        BigDecimal dailyLimit,
        BigDecimal monthlyLimit,
        LocalDateTime expiresAt
) {
}
