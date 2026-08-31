package cardservice.dto;

import enums.card.CardStatus;
import enums.common.Currency;
import java.time.LocalDateTime;
import java.util.UUID;

public record UpdateCardResult(
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
