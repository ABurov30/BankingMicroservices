package apigateway.dto.card;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record GetCardByAccountIdRequestDto(@NotNull UUID accountId) {}
