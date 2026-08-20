package apigateway.listener;

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
public class NotificationSubscriptionListener {
  private static final String NOTIFICATIONS_DESTINATION = "/user/queue/notifications";
  private static final Logger log = LoggerFactory.getLogger(NotificationSubscriptionListener.class);

  private final Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();

  @EventListener
  public void onSubscribe(SessionSubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

    String destination = accessor.getDestination();
    if (!NOTIFICATIONS_DESTINATION.equals(destination)) {
      return;
    }

    String subscriptionId = accessor.getSubscriptionId();
    String sessionId = accessor.getSessionId();
    Principal principal = accessor.getUser();
    if (subscriptionId == null || sessionId == null || principal == null) {
      log.warn(
          "Reject notification sub: missing metadata, dest={}, sid={}, sub={}, principal={}",
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
          "Reject notification sub: invalid principal, dest={}, sid={}, sub={}, principal={}",
          destination,
          sessionId,
          subscriptionId,
          principal.getName());
      return;
    }

    String key = sessionId + ":" + subscriptionId;
    subscriptions.put(key, new Subscription(authUserId));

    log.info(
        "Notification subscription opened: user={}, sid={}, sub={}",
        authUserId,
        sessionId,
        subscriptionId);
  }

  @EventListener
  public void onUnsubscribe(SessionUnsubscribeEvent event) {
    StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());

    String key = accessor.getSessionId() + ":" + accessor.getSubscriptionId();
    Subscription subscription = subscriptions.remove(key);

    if (subscription != null) {
      log.info(
          "Notification subscription closed: user={}, sid={}, sub={}",
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

              closedCount.incrementAndGet();
              return true;
            });

    if (closedCount.get() > 0) {
      log.info(
          "Notification subscriptions closed on disconnect: sid={}, count={}",
          sessionId,
          closedCount.get());
    }
  }

  private record Subscription(UUID authUserId) {}
}
