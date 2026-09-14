package apigateway.dto.request.auth;

import java.util.List;
import java.util.UUID;

public record GetAuthUserByIdsRequestDto(List<UUID> authUserIds) {}
