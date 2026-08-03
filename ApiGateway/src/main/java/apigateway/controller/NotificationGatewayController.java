package apigateway.controller;

import apigateway.client.NotificationGrpcClient;
import apigateway.config.CookieConfig;
import apigateway.dto.notification.GetNotificationsRequestDto;
import apigateway.dto.notification.MarkNotificationsAsReadedCommand;
import apigateway.dto.notification.MarkNotificationsAsReadedRequestDto;
import apigateway.dto.notification.NotificationResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notification")
public class NotificationGatewayController {

    private final NotificationGrpcClient notificationClient;
    private final CookieConfig cookieConfig;

    public NotificationGatewayController(NotificationGrpcClient notificationClient, CookieConfig cookieConfig) {
        this.notificationClient = notificationClient;
        this.cookieConfig = cookieConfig;
    }

    @GetMapping("/health")
    public String getNotificationHealth() {
        return notificationClient.getNotificationHealth();
    }

    @GetMapping("/notifications")
    public List<NotificationResponseDto> getNotifications(HttpServletRequest request) {
        UUID authUserId = UUID.fromString(cookieConfig.getAccessTokenJwt(request).getSubject());

        return notificationClient.getNotifications(new GetNotificationsRequestDto(authUserId));
    }

    @PatchMapping("/notifications/mark-as-readed")
    public void markAsReaded(
            HttpServletRequest request,
            @Valid @RequestBody MarkNotificationsAsReadedRequestDto markRequest
    ) {
        UUID authUserId = UUID.fromString(cookieConfig.getAccessTokenJwt(request).getSubject());

        notificationClient.markAsReaded(new MarkNotificationsAsReadedCommand(authUserId, markRequest.ids()));
    }
}
