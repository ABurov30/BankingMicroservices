package apigateway.dto.card;

import java.util.UUID;

public record GetCardByAccountIdRequestDto(
        UUID accountId
) {
}
