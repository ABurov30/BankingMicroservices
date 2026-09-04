package apigateway.dto.transaction;

import enums.common.Currency;
import enums.transaction.TransactionStatus;
import java.util.UUID;

public record CreateTransactionResponseDto(
    UUID transactionId, Long minorUnits, Currency currency, TransactionStatus status) {}
