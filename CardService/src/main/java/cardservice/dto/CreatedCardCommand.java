package cardservice.dto;

import enums.common.Currency;
import java.util.UUID;
import org.springframework.lang.Nullable;

public record CreatedCardCommand(
    UUID accountId,
    Currency currency,
    @Nullable UUID authUserId,
    @Nullable String accountNumber,
    @Nullable String role) {}
