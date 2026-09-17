# AccountService Data and Persistence

[Docs Index](README.md)

## Storage

Primary storage is PostgreSQL. Schema changes are managed by Liquibase under `src/main/resources/db/changelog`.

## Entities

| Entity | Purpose |
| --- | --- |
| `AccountEntity` | Bank account state and balance |
| `AccountInterestAccrualEntity` | Daily savings interest history, unique per account and date |
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
The account outbox also permits `CACHE_INVALIDATION`, used for account and transaction read-model cache eviction.

Funds reservation rejects transactions whose requested currency does not match the source account
currency. The hold stores the source account currency used for the reserved minor-unit amount.

Consumed Kafka events atomically claim their unique `event_key` in `processed_events` before the
handler runs. The claim and business operation share one database transaction: duplicates are
skipped, while a handler failure rolls back both changes. The service exports skipped-event counts
through the `kafka.idempotency.duplicates` metric.

Currency arithmetic should use `moneyunitsconverter.MoneyUnitsConverter` from `com.burov:support`
when converting between major and minor units.

The `processed_events.event_key` column stores the incoming `eventId` header,
not the Kafka partition key. Its unique constraint continues to deduplicate event retries.

`AccountInterestAccrualRepository` stores interest history introduced by
`009-create-account-interest-accruals.sql`. Its unique `(account_id, accrual_date)`
constraint protects against duplicate accruals. History and balance changes share
one transaction. See [interest accrual](interest-accrual.md) for calculation and retry semantics.

## Outbox attempts

The outbox status constraint now permits PROCESSING. The `*-outbox-processing.sql` migration
adds partial pending and lease indexes. `locked_by` is a unique attempt token;
`retry_count` increments on claim. Claims and callbacks use separate transactions.
See [shared outbox publishing](../../docs/outbox.md) for settings, guarantees, metrics and rollout.
