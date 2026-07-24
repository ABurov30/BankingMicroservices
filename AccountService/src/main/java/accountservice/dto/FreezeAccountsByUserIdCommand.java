package accountservice.dto;

import java.util.UUID;

public record FreezeAccountsByUserIdCommand(
        UUID userId
) {
}
