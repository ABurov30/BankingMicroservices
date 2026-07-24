package apigateway.dto.card;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateCardRequestDto(
        @NotNull
        UUID accountId
) {
}
