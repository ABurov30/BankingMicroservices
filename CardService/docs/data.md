# CardService Data and Persistence

[Docs Index](README.md)

## Storage

Primary storage is PostgreSQL. Schema changes are managed by Liquibase under `src/main/resources/db/changelog`.

## Entities

| Entity | Purpose |
| --- | --- |
| `CardEntity` | Card state, limits, and card metadata |
| `CardLimitHoldEntity` | Reserved card limit spend for in-flight transactions |
| `AccountOwnershipProjectionEntity` | Local projection of account ownership needed for card operations |
| `CardOutboxEventEntity` | Outbox rows for card-domain events |
| `ProcessedEventEntity` | Idempotency tracking for consumed Kafka events |

## Repositories

- `CardRepository`
- `CardLimitHoldRepository`
- `AccountOwnershipProjectionRepository`
- `CardOutboxEventRepository`
- `ProcessedEventRepository`

## Migration Files

- `001-create-card-tables.sql`
- `002-create-proccessed-event-table.sql`
- `003-create-card-limit-hold-table.sql`

## Data Integrity Notes

Account ownership projection is derived data. Do not mutate it from request paths unless the change corresponds to an upstream account event.

`spendDailyLimit` and `spendMonthlyLimit` are persisted counters on `cards`. They are increased when card limits are reserved for a transaction, released by compensation or timeout, and reset by scheduled daily and monthly jobs.

`card_limit_holds.transaction_id` is unique and provides idempotency for card limit reservations.
