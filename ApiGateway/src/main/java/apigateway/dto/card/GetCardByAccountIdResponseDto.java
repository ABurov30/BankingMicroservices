package apigateway.dto.card;

import enums.card.CardStatus;
import enums.common.Currency;
import java.time.LocalDateTime;
import java.util.UUID;

public record GetCardByAccountIdResponseDto(
    UUID cardId,
    UUID accountId,
    String pan,
    CardStatus status,
    Long dailyLimitMinorUnits,
    Long monthlyLimitMinorUnits,
    LocalDateTime expiresAt,
    Long spendDailyLimitMinorUnits,
    Long spendMonthlyLimitMinorUnits,
    Currency currency) {}
