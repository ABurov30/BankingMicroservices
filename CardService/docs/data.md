# CardService Data and Persistence

[Docs Index](README.md)

## Storage

Primary storage is PostgreSQL. Schema changes are managed by Liquibase under `src/main/resources/db/changelog`.

## Entities

| Entity | Purpose |
| --- | --- |
| `CardEntity` | Card state, limits, and card metadata |
| `AccountOwnershipProjectionEntity` | Local projection of account ownership needed for card operations |
| `CardOutboxEventEntity` | Outbox rows for card-domain events |
| `ProcessedEventEntity` | Idempotency tracking for consumed Kafka events |

## Repositories

- `CardRepository`
- `AccountOwnershipProjectionRepository`
- `CardOutboxEventRepository`
- `ProcessedEventRepository`

## Migration Files

- `001-create-card-tables.sql`
- `002-create-proccessed-event-table.sql`

## Data Integrity Notes

Account ownership projection is derived data. Do not mutate it from request paths unless the change corresponds to an upstream account event.
