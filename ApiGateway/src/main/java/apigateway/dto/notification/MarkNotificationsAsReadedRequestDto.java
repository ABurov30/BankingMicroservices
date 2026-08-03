package apigateway.dto.notification;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record MarkNotificationsAsReadedRequestDto(
        @NotEmpty List<UUID> ids
) {
}
