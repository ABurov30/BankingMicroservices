package apigateway.dto.command.notification;

import java.util.List;
import java.util.UUID;

public record MarkNotificationsAsReadedCommand(UUID authUserId, List<UUID> ids) {}
