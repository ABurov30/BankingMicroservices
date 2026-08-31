package apigateway.dto.card;

import enums.card.CardStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record UpdateCardRequestDto(
    @NotNull UUID accountId,
    @NotNull UUID cardId,
    @NotNull CardStatus status,
    @NotNull @Positive Long dailyLimitMinorUnits,
    @NotNull @Positive Long monthlyLimitMinorUnits) {}
