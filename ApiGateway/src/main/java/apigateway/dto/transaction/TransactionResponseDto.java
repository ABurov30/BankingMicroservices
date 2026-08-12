package apigateway.dto.transaction;

import apigateway.dto.account.GetAccountResponseDto;
import enums.account.AccountCurrency;
import enums.transaction.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponseDto(
        BigDecimal amount,
        AccountCurrency currency,
        TransactionStatus status,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        GetAccountResponseDto sourceAccount,
        GetAccountResponseDto targetAccount) {
}
