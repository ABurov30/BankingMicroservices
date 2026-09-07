package apigateway.dto.response.transaction;

import enums.common.Currency;

public record TransactionStatusAccountResponseDto(String accountNumber, Currency currency) {}
