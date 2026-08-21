package apigateway.dto.transaction;

import enums.common.Currency;

public record TransactionStatusAccountResponseDto(String accountNumber, Currency currency) {}
