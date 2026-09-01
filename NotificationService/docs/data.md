# NotificationService Data and Persistence

[Docs Index](README.md)

## Storage

The service uses both PostgreSQL and MongoDB.

PostgreSQL is used for migration-managed relational state such as processed event tracking and push notification outbox rows. MongoDB stores notification documents.

## Documents and Entities

| Type | Purpose |
| --- | --- |
| `EmailNotificationDocument` | Email notification document |
| `PushNotificationDocument` | Push notification document |
| `ProcessedEventEntity` | Idempotency tracking for consumed Kafka events |
| `PushNotificationOutboxEventEntity` | Outbox rows for push notification Kafka events |

## Repositories

- `EmailNotificationRepository`
- `PushNotificationRepository`
- `ProcessedEventRepository`
- `PushNotificationOutboxEventRepository`

## Migration Files

- `001-create-notification-tables.sql`
- `002-create-processed-event-table.sql`

## Email Templates

Templates live under `src/main/resources/templates/email` and include auth lifecycle templates such as user created, blocked, unlocked, verified, and forget-password flows.

## Data Integrity Notes

Notification creation should remain idempotent for consumed Kafka events through the
`processedevent` helpers in `com.burov:support`. Push notification outbox rows are the source for
events delivered to `ApiGateway`.

Transaction notification amounts are converted from minor units to major units with
`moneyunitsconverter.MoneyUnitsConverter` before being stored in notification payloads.
