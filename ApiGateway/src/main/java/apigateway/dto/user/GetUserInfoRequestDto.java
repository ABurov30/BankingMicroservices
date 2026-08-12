package apigateway.dto.user;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record GetUserInfoRequestDto(@NotBlank UUID authUserId) {}
