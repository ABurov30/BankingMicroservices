package transactionservice.dto;

import enums.common.Currency;
import enums.transaction.TransactionStatus;
import java.util.UUID;

public record CreateTransactionResult(
    UUID transactionId, Long minorUnits, Currency currency, TransactionStatus status) {}
