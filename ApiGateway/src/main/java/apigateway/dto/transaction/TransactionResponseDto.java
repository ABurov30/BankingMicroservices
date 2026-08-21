package apigateway.dto.transaction;

import apigateway.dto.account.GetAccountResponseDto;
import enums.common.Currency;
import enums.transaction.TransactionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponseDto(
    UUID transactionId,
    BigDecimal minorUnits,
    Currency currency,
    TransactionStatus status,
    LocalDateTime createdAt,
    LocalDateTime completedAt,
    GetAccountResponseDto sourceAccount,
    GetAccountResponseDto targetAccount) {}
