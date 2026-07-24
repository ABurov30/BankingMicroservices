package cardservice.dto;

import java.util.UUID;

public record FreezeCardsCommand(
        UUID accountId
) {
}
