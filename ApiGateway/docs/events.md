# ApiGateway Events

[Docs Index](README.md)

## Consumed Kafka Events

| Event | Source | Handler |
| --- | --- | --- |
| `PUSH_NOTIFICATION_CREATED` | `NotificationService` | `GatewayKafkaListener.handlePushNotificationCreated` |
| `CACHE_INVALIDATION` | Domain services | `GatewayKafkaListener.handleCacheInvalidation` |

## Event Handling Flow

1. The Kafka listener receives `NotificationCreatedEventPayload`.
2. The payload is mapped to a gateway notification response DTO.
3. The gateway sends the notification to the authenticated user's WebSocket queue.

## Cache Invalidation

`CACHE_INVALIDATION` is a cross-service cache-coherence event. Its payload contains `keys`, the
list of cache keys that are no longer valid. Producers create the event in their transactional
outbox so it is published only after the corresponding domain change commits.

The Gateway consumes an event once per Kafka consumer group and performs the following steps in
order:

1. Delete every listed key from Redis L2.
2. Publish every key to the Redis `cache:l1-invalidation` channel.
3. Each live Gateway instance subscribed to that channel evicts the key from its local Caffeine
   L1 cache.

This ensures that Redis is cleared before any instance handles a cache miss after local
invalidation. Redis Pub/Sub is intentionally used only for the live-instance L1 broadcast; Kafka
and the transactional outbox provide durable delivery of the original invalidation event.

## Producer Behavior

`ApiGateway` is currently a consumer only. It does not publish domain events.

## Agent Notes

If the push notification payload changes, update both `GatewayKafkaListener` and `NotificationDtoMapper`, then verify the matching producer in `NotificationService`.
