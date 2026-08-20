package apigateway.dto.transaction;

import enums.account.AccountCurrency;

public record TransactionStatusAccountResponseDto(String accountNumber, AccountCurrency currency) {}
