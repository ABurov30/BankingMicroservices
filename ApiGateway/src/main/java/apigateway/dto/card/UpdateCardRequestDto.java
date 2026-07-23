package apigateway.dto.card;

import enums.card.CardStatus;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateCardRequestDto(
        @NotBlank
        UUID cardId,
        CardStatus status,
        BigDecimal dailyLimit,
        BigDecimal monthlyLimit
) {
}
