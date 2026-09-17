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
- `004-store-card-limits-in-minor-units.sql`
- `005-add-card-currency.sql`
- `006-use-minor-units-for-card-limits.sql`

## Data Integrity Notes

Account ownership projection is derived data. Do not mutate it from request paths unless the change corresponds to an upstream account event.
The card outbox also permits `CACHE_INVALIDATION`, used when card changes invalidate account overview caches.

Card limits, card spend counters, and card limit holds are stored as minor-unit amounts. `spendDailyLimitMinorUnits` and `spendMonthlyLimitMinorUnits` are persisted counters on `cards`. They are increased when card limits are reserved for a transaction, released by compensation or timeout, and reset by scheduled daily and monthly jobs.

`cards.currency` is required, stores the card account currency, and is constrained to `USD`, `EUR`,
`CNY`, or `GBP`. Card limit reservation rejects transactions whose requested currency does not match
the card currency.

`account_ownership_projection.currency` is required, stores the source account currency received
from account events, and is constrained to `USD`, `EUR`, `CNY`, or `GBP`. Card responses expose the
card/account currency.

`card_limit_holds.transaction_id` is unique and provides idempotency for card limit reservations.

`CardLimitReservationService.reserve` runs in one Spring transaction. It acquires a
`PESSIMISTIC_WRITE` lock through `CardRepository.findByIdForUpdate` before checking available
daily and monthly limits. The hold insert and both spend counter updates commit together;
the card lock remains held until the transaction ends. Concurrent reservations for the same
card therefore check counters that include the preceding committed reservation.

The unique transaction ID constraint also protects concurrent duplicate requests: the initial
existence check alone cannot prevent that race. A duplicate insert or another database failure
rolls back the hold and counter changes. The non-transactional `CardService` wrapper catches
exceptions outside the reservation bean's transaction boundary, including commit failures,
and returns `FAILED`. A duplicate request returns `FAILED`, rather than replaying `RESERVED`.

Consumed Kafka events atomically claim their unique `event_key` in `processed_events` before the
handler runs. The claim and business operation share one database transaction: duplicates are
skipped, while a handler failure rolls back both changes. The service exports skipped-event counts
through the `kafka.idempotency.duplicates` metric.

Migration `007-drop-outbox-event-key-unique-constraint.sql` removes uniqueness from the outbox routing key;
multiple events for one aggregate must coexist. The primary key still identifies each event.

The `processed_events.event_key` column stores the incoming `eventId` header,
not the Kafka partition key. Its unique constraint continues to deduplicate event retries.

## Outbox attempts

The outbox status constraint now permits PROCESSING. The `*-outbox-processing.sql` migration
adds partial pending and lease indexes. `locked_by` is a unique attempt token;
`retry_count` increments on claim. Claims and callbacks use separate transactions.
See [shared outbox publishing](../../docs/outbox.md) for settings, guarantees, metrics and rollout.
