# UserService Data and Persistence

[Docs Index](README.md)

## Storage

Primary storage is PostgreSQL. Schema changes are managed by Liquibase under `src/main/resources/db/changelog`.

## Entities

| Entity | Purpose |
| --- | --- |
| `UserProfileEntity` | User profile projection |
| `UserOutboxEventEntity` | Outbox rows for user profile Kafka events |
| `ProcessedEventEntity` | Idempotency tracking for consumed Kafka events |

## Repositories

- `UserProfileRepository`
- `UserOutboxEventRepository`
- `ProcessedEventRepository`

## Migration Files

- `001-create-user-tables.sql`
- `002-update-user-outbox-event-types.sql`
- `003-add-user-profile-role.sql`
- `004-drop-user-outbox-event-key-unique-constraint.sql`
- `005-create-processed-event-table.sql`

## Data Integrity Notes

The service atomically claims each consumed Kafka event's unique `event_key` in `processed_events`
before its handler runs. The claim and business operation share one database transaction:
duplicates are skipped, while a handler failure rolls back both changes. The service exports
skipped-event counts through the `kafka.idempotency.duplicates` metric. When adding new listeners,
include idempotency handling rather than applying event payloads directly.
