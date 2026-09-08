package apigateway.controller;

import apigateway.client.NotificationGrpcClient;
import apigateway.config.CookieConfig;
import apigateway.dto.command.notification.MarkNotificationsAsReadedCommand;
import apigateway.dto.request.notification.GetNotificationsRequestDto;
import apigateway.dto.request.notification.MarkNotificationsAsReadedRequestDto;
import apigateway.dto.response.notification.NotificationResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationGatewayController {

  private final NotificationGrpcClient notificationClient;
  private final CookieConfig cookieConfig;

  @GetMapping("/health")
  public String getNotificationHealth() {
    return notificationClient.getNotificationHealth();
  }

  @GetMapping("/notifications")
  public List<NotificationResponseDto> getNotifications(HttpServletRequest request) {
    return notificationClient.getNotifications(
        new GetNotificationsRequestDto(cookieConfig.getAuthUserId(request)));
  }

  @PatchMapping("/notifications/mark-as-readed")
  public void markAsReaded(
      HttpServletRequest request,
      @Valid @RequestBody MarkNotificationsAsReadedRequestDto markRequest) {
    notificationClient.markAsReaded(
        new MarkNotificationsAsReadedCommand(
            cookieConfig.getAuthUserId(request), markRequest.ids()));
  }
}
