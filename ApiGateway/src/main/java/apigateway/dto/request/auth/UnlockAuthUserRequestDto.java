package apigateway.dto.request.auth;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UnlockAuthUserRequestDto(@NotNull UUID authUserId) {}
