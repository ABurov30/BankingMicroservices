package apigateway.dto.card;

import java.util.UUID;

public record CreateCardRequestDto(
        UUID accountId
) {
}
