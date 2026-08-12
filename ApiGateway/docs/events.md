# ApiGateway Events

[Docs Index](README.md)

## Consumed Kafka Events

| Event | Source | Handler |
| --- | --- | --- |
| `PUSH_NOTIFICATION_CREATED` | `NotificationService` | `GatewayKafkaListener.handlePushNotificationCreated` |

## Event Handling Flow

1. The Kafka listener receives `NotificationCreatedEventPayload`.
2. The payload is mapped to a gateway notification response DTO.
3. The gateway sends the notification to the authenticated user's WebSocket queue.

## Producer Behavior

`ApiGateway` is currently a consumer only. It does not publish domain events.

## Agent Notes

If the push notification payload changes, update both `GatewayKafkaListener` and `NotificationDtoMapper`, then verify the matching producer in `NotificationService`.
