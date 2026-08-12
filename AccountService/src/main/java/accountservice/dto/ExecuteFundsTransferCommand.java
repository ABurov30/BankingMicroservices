package accountservice.dto;

import accountservice.entity.AccountHoldEntity;
import java.util.UUID;

public record ExecuteFundsTransferCommand(
    UUID transactionId,
    UUID targetAccountId,
    UUID targetAuthUserId,
    AccountHoldEntity accountHold) {}
