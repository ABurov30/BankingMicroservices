package accountservice.dto;

import java.util.UUID;

public record FreezeAccountCommand(
        UUID accountId
) {
}
