package apigateway.dto.transaction;

import enums.common.Currency;
import enums.transaction.TransactionStatus;

public record TransactionStatusResponseDto(
    Long minorUnits,
    Currency currency,
    TransactionStatus status,
    TransactionStatusAccountResponseDto sourceAccount,
    TransactionStatusAccountResponseDto targetAccount) {}
