package apigateway.dto.response.transaction;

import apigateway.dto.response.account.GetRecipientResponseDto;
import enums.common.Currency;
import enums.transaction.TransactionStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponseDto(
    UUID transactionId,
    Long minorUnits,
    Currency currency,
    TransactionStatus status,
    LocalDateTime createdAt,
    LocalDateTime completedAt,
    GetRecipientResponseDto sourceAccount,
    GetRecipientResponseDto targetAccount) {}
