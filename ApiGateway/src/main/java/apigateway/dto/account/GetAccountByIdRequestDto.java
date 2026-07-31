package apigateway.dto.account;

import java.util.UUID;

public record GetAccountByIdRequestDto(
        UUID accountId
) {
}
