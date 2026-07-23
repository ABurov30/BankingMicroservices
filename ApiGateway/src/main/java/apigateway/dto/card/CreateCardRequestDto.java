package apigateway.dto.card;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateCardRequestDto(
        @NotBlank
        UUID accountId
) {
}
