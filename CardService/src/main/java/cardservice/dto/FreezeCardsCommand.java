package cardservice.dto;

import java.util.UUID;

public record FreezeCardsCommand(
        UUID accountId,
        UUID authUserId,
        String accountNumber
) {
}
