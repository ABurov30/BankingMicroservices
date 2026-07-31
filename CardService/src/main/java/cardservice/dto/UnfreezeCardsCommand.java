package cardservice.dto;

import java.util.UUID;

public record UnfreezeCardsCommand(
        UUID accountId
) {
}
