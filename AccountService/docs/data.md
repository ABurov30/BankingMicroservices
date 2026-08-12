# AccountService Data and Persistence

[Docs Index](README.md)

## Storage

Primary storage is PostgreSQL. Schema changes are managed by Liquibase under `src/main/resources/db/changelog`.

## Entities

| Entity | Purpose |
| --- | --- |
| `AccountEntity` | Bank account state and balance |
| `AccountHoldEntity` | Reserved funds for transaction processing |
| `CurrencyEntity` | Currency reference data |
| `AccountOutboxEventEntity` | Outbox rows for account-domain events |
| `ProcessedEventEntity` | Idempotency tracking for consumed Kafka events |

## Repositories

- `AccountRepository`
- `AccountHoldRepository`
- `CurrencyRepository`
- `AccountOutboxEventRepository`
- `ProcessedEventRepository`

## Migration Files

- `001-create-account-tables.sql`
- `002-drop-account-outbox-event-key-unique-constraint.sql`
- `003-create-processed-event-table.sql`
- `004-create-currency-table.sql`
- `005-remove-credit-account-type.sql`
- `006-create-account-hold-table.sql`

## Data Integrity Notes

Balance mutation, hold creation, and hold release are financial operations. Keep them transactional and verify compensation behavior when changing them.
