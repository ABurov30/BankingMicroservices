package apigateway.dto.transaction;

import enums.account.AccountCurrency;
import enums.transaction.TransactionStatus;
import java.math.BigDecimal;

public record TransactionStatusResponseDto(
    BigDecimal amount,
    AccountCurrency currency,
    TransactionStatus status,
    TransactionStatusAccountResponseDto sourceAccount,
    TransactionStatusAccountResponseDto targetAccount) {}
