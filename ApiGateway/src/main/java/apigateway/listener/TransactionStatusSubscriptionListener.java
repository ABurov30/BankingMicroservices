package apigateway.listener;

import apigateway.service.TransactionStatusStreamService;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

@Component
public class TransactionStatusSubscriptionListener {
  private static final String DESTINATION_PREFIX = "/user/queue/transactions/";
  private static final Logger log =
      LoggerFactory.getLogger(TransactionStatusSubscriptionListener.class);

  private final TransactionStatusStreamService streamService;
  private final Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();

  public TransactionStatusSubscriptionListener(TransactionStatusStreamService statusStreamService) {
    this.streamService = statusStreamService;
  }

  @EventListener
  public void onSubscribe(SessionSubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

    String destination = accessor.getDestination();
    if (destination == null || !destination.startsWith(DESTINATION_PREFIX)) {
      return;
    }

    String subscriptionId = accessor.getSubscriptionId();
    String sessionId = accessor.getSessionId();
    Principal principal = accessor.getUser();
    if (subscriptionId == null || sessionId == null || principal == null) {
      log.warn(
          "Reject tx status subscription: missing metadata, dest={}, sid={}, sub={}, principal={}",
          destination,
          sessionId,
          subscriptionId,
          principal != null);
      return;
    }

    UUID authUserId;
    try {
      authUserId = UUID.fromString(principal.getName());
    } catch (IllegalArgumentException exception) {
      log.warn(
          "Reject tx status subscription: invalid principal, dest={}, sid={}, sub={}, principal={}",
          destination,
          sessionId,
          subscriptionId,
          principal.getName());
      return;
    }

    UUID subscriptionKey = UUID.randomUUID();
    UUID transactionId;
    try {
      transactionId = UUID.fromString(destination.substring(DESTINATION_PREFIX.length()));
    } catch (IllegalArgumentException exception) {
      log.warn(
          "Reject tx status subscription: invalid destination, dest={}, sid={}, sub={}, user={}",
          destination,
          sessionId,
          subscriptionId,
          authUserId);
      return;
    }

    String key = sessionId + ":" + subscriptionId;
    subscriptions.put(key, new Subscription(authUserId, transactionId));

    log.info(
        "Tx status subscription opened: tx={}, user={}, sid={}, sub={}, stream={}",
        transactionId,
        authUserId,
        sessionId,
        subscriptionId,
        subscriptionKey);
    streamService.watch(transactionId, authUserId, subscriptionKey, key);
  }

  @EventListener
  public void onUnsubscribe(SessionUnsubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

    String key = accessor.getSessionId() + ":" + accessor.getSubscriptionId();
    Subscription subscription = subscriptions.remove(key);

    if (subscription != null) {
      streamService.unwatch(key);
      log.info(
          "Tx status subscription closed: tx={}, user={}, sid={}, sub={}",
          subscription.transactionId(),
          subscription.authUserId(),
          accessor.getSessionId(),
          accessor.getSubscriptionId());
    }
  }

  @EventListener
  public void onDisconnect(SessionDisconnectEvent event) {
    String sessionId = event.getSessionId();

    AtomicLong closedCount = new AtomicLong();
    subscriptions
        .entrySet()
        .removeIf(
            entry -> {
              if (!entry.getKey().startsWith(sessionId + ":")) {
                return false;
              }

              streamService.unwatch(entry.getKey());
              closedCount.incrementAndGet();
              return true;
            });
    if (closedCount.get() > 0) {
      log.info(
          "Tx status subscriptions closed on disconnect: sid={}, count={}",
          sessionId,
          closedCount.get());
    }
  }

  private record Subscription(UUID authUserId, UUID transactionId) {}
}
