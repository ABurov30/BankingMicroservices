package apigateway.dto.request.user;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record GetUserInfoByManagerRequestDto(@NotNull UUID userId) {}
