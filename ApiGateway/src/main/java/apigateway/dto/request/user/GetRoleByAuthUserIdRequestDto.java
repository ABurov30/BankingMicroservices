package apigateway.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record GetRoleByAuthUserIdRequestDto(@NotBlank UUID authUserId) {}
