package apigateway.listener;

import apigateway.mapper.result.NotificationResultMapper;
import kafkacontracts.account.NotificationCreatedEventPayload;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GatewayKafkaListener {
  private static final Logger log = LoggerFactory.getLogger(GatewayKafkaListener.class);

  private final SimpMessagingTemplate messagingTemplate;
  private final NotificationResultMapper notificationResultMapper;

  @KafkaListener(
      topics =
          "#{T(kafkacontracts.notification.NotificationEventType)"
              + ".PUSH_NOTIFICATION_CREATED.getTopic()}")
  public void handlePushNotificationCreated(NotificationCreatedEventPayload payload) {
    log.debug(
        "Forwarding push notification to WebSocket user destination: authUserId={}, type={}",
        payload.getAuthUserId(),
        payload.getType());
    messagingTemplate.convertAndSendToUser(
        payload.getAuthUserId().toString(),
        "/queue/notifications",
        notificationResultMapper.toNotificationResponseDto(payload));
  }
}
