# TransactionService Data and Persistence

[Docs Index](README.md)

## Storage

Primary storage is PostgreSQL. Schema changes are managed by Liquibase under `src/main/resources/db/changelog`.

## Entities

| Entity | Purpose |
| --- | --- |
| `TransactionEntity` | Transaction state and transaction data |
| `TransactionOutboxEventEntity` | Outbox rows for transaction-domain events |
| `ProcessedEventEntity` | Idempotency tracking for consumed Kafka events |

## Repositories

- `TransactionRepository`
- `TransactionOutboxEventRepository`
- `ProcessedEventRepository`

## Migration Files

- `001-create-transaction-tables.sql`

## Data Integrity Notes

Transaction state, card limit reservation state, and account reservation state are coupled through events and gRPC calls. Avoid changing one side of the flow without checking failure and compensation behavior.
