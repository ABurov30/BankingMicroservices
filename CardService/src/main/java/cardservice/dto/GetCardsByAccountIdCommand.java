package cardservice.dto;

import java.util.UUID;

public record GetCardsByAccountIdCommand(
        UUID accountId
) {
}
