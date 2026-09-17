package apigateway.listener;

import apigateway.cache.LocalCacheInvalidationPublisher;
import apigateway.cache.RedisCacheService;
import apigateway.mapper.result.NotificationResultMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import kafkacontracts.account.NotificationCreatedEventPayload;
import kafkacontracts.cache.CacheInvalidationEventPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import processedevent.annotation.EventKey;
import processedevent.annotation.IdempotentKafkaEvent;

@Service
public class GatewayKafkaListener {
  private static final Logger log = LoggerFactory.getLogger(GatewayKafkaListener.class);

  private final SimpMessagingTemplate messagingTemplate;
  private final NotificationResultMapper notificationResultMapper;

  private final RedisCacheService redisCacheService;
  private final LocalCacheInvalidationPublisher publisher;
  private final Counter cacheInvalidationEvents;
  private final Counter cacheInvalidationKeys;

  public GatewayKafkaListener(
      SimpMessagingTemplate messagingTemplate,
      NotificationResultMapper notificationResultMapper,
      RedisCacheService redisCacheService,
      LocalCacheInvalidationPublisher publisher,
      MeterRegistry meters) {
    this.messagingTemplate = messagingTemplate;
    this.notificationResultMapper = notificationResultMapper;
    this.redisCacheService = redisCacheService;
    this.publisher = publisher;
    this.cacheInvalidationEvents = meters.counter("cache.invalidation.events");
    this.cacheInvalidationKeys = meters.counter("cache.invalidation.keys");
  }

  @IdempotentKafkaEvent
  @KafkaListener(
      topics =
          "#{T(kafkacontracts.notification.NotificationEventType)"
              + ".PUSH_NOTIFICATION_CREATED.getTopic()}")
  public void handlePushNotificationCreated(
      NotificationCreatedEventPayload payload, @EventKey @Header("eventId") String eventId) {
    log.debug(
        "Forwarding push notification to WebSocket user destination: authUserId={}, type={}",
        payload.getAuthUserId(),
        payload.getType());
    messagingTemplate.convertAndSendToUser(
        payload.getAuthUserId().toString(),
        "/queue/notifications",
        notificationResultMapper.toNotificationResponseDto(payload));
  }

  @IdempotentKafkaEvent
  @KafkaListener(topics = "#{T(kafkacontracts.cache.CacheEventType).CACHE_INVALIDATION.getTopic()}")
  public void handleCacheInvalidation(
      CacheInvalidationEventPayload payload, @EventKey @Header("eventId") String eventId) {
    cacheInvalidationEvents.increment();
    cacheInvalidationKeys.increment(payload.getKeys().size());

    payload.getKeys().forEach(redisCacheService::evict);
    payload.getKeys().forEach(publisher::publish);
  }
}
