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

Consumed Kafka events use processed-event tracking through the `processedevent` helpers in
`com.burov:support`.

Migration `003-drop-outbox-event-key-unique-constraint.sql` removes uniqueness from the outbox routing key;
multiple events for one aggregate must coexist. The primary key still identifies each event.

The `processed_events.event_key` column stores the incoming `eventId` header,
not the Kafka partition key. Its unique constraint continues to deduplicate event retries.

## Outbox attempts

The outbox status constraint now permits PROCESSING. The `*-outbox-processing.sql` migration
adds partial pending and lease indexes. `locked_by` is a unique attempt token;
`retry_count` increments on claim. Claims and callbacks use separate transactions.
See [shared outbox publishing](../../docs/outbox.md) for settings, guarantees, metrics and rollout.
