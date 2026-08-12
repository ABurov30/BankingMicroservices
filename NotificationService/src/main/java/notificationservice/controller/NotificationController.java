package notificationservice.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import notificationservice.dto.GetPushNotificationResult;
import notificationservice.dto.MarkPushNotificationsAsReadedRequest;
import notificationservice.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notification")
public class NotificationController {
  private final NotificationService notificationService;

  public NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  @GetMapping("/notifications")
  public List<GetPushNotificationResult> getNotifications(@RequestParam UUID authUserId) {
    return notificationService.getPushNotifications(authUserId);
  }

  @PatchMapping("/notifications/mark-as-readed")
  public void markAsReaded(@Valid @RequestBody MarkPushNotificationsAsReadedRequest request) {
    notificationService.markPushNotificationsAsReaded(request.authUserId(), request.ids());
  }
}
