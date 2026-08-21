package apigateway.dto.transaction;

import enums.common.Currency;
import enums.transaction.TransactionStatus;
import java.math.BigDecimal;

public record TransactionStatusResponseDto(
    BigDecimal minorUnits,
    Currency currency,
    TransactionStatus status,
    TransactionStatusAccountResponseDto sourceAccount,
    TransactionStatusAccountResponseDto targetAccount) {}
