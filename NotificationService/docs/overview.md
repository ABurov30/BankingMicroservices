# NotificationService Overview

[Docs Index](README.md)

## Responsibility

`NotificationService` handles user-facing notifications. It consumes domain events, resolves notification payloads, stores notification records, sends email notifications, and emits push notification events for gateway WebSocket delivery.

## Runtime Role

- Runs as `notification-service`.
- Exposes HTTP on `NOTIFICATION_PORT`, normally `8086`.
- Exposes gRPC on `NOTIFICATION_GRPC_PORT`, normally `8095`.
- Uses PostgreSQL for processed events and outbox state.
- Uses MongoDB for notification documents.
- Consumes auth, account, card, and transaction events.
- Sends email through SMTP.
- Publishes push notification events consumed by `ApiGateway`.
- Transaction failure notifications are based on the failed amount, currency, and authenticated user
  because the failure event does not include account details.

## Important Packages

- `controller` - HTTP notification endpoints.
- `grpc` - gRPC health endpoint and exception interceptor.
- `listener` - Kafka consumers for domain events.
- `service.email` - email sending, scheduling, and template resolution.
- `service.push` - push notification resolution and outbox publishing.
- `document` - Mongo notification documents.
- `entity` - PostgreSQL processed-event and push-outbox entities.
- `mapper` - command and event payload mapping.

## Integration Boundaries

The service should not own domain decisions about auth, account, card, or transaction state. It translates those events into notifications.
