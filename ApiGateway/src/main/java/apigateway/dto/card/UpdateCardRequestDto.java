package apigateway.dto.card;

import enums.card.CardStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateCardRequestDto(
        @NotNull
        UUID accountId,
        @NotNull
        UUID cardId,
        @NotNull
        CardStatus status,
        @NotNull
        BigDecimal dailyLimit,
        @NotNull
        BigDecimal monthlyLimit
) {
}
