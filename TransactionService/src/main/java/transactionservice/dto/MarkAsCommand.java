package transactionservice.dto;

import enums.transaction.TransactionStatus;
import java.util.UUID;

public record MarkAsCommand(UUID transactionId, TransactionStatus status) {}
