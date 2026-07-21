package cardservice.dto;

import java.util.UUID;

public record CreatedCardCommand(
        UUID accountId
) {
}
