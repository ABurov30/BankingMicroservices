# CardService Events

[Docs Index](README.md)

## Consumed Events

| Event | Source | Purpose |
| --- | --- | --- |
| `ACCOUNT_CREATED` | `AccountService` | Create a card and store account ownership and currency in the local projection |
| `ACCOUNT_FROZEN` | `AccountService` | Freeze related card availability |
| `ACCOUNT_UNFROZEN` | `AccountService` | Restore related card availability |
| `TRANSACTION_COMPLETED` | `AccountService` | Mark card limit reservation as released after a completed transaction |
| `TRANSACTION_COMPENSATED` | `AccountService` | Release reserved card limits for a compensated transaction |
| `TRANSACTION_CARD_LIMIT_HOLD_COMPENSATION` | `TransactionService` | Compensate a card limit hold when AccountService rejects funds reservation |

`TRANSACTION_CARD_LIMIT_HOLD_COMPENSATION` carries `transactionId`. CardService handles the event
idempotently: a `RESERVED` hold releases its daily and monthly limits and becomes `COMPENSATED`;
an absent or non-reserved hold is a no-op. The event is published by TransactionService from its
outbox after it records the failed reservation.

## Produced Events

Card changes are published through `CardOutboxPublisher`.

Known produced event categories include:

- card created
- card frozen
- card unfrozen
- card limit hold released by time (contains `transactionId`; emitted transactionally by the expiry scheduler)

## Consumers

`NotificationService` consumes card events for user notifications.

## Agent Notes

Kafka consumers treat stale account/card state and card-limit hold events as no-ops: they log the current state and return instead of retrying the same event.

If account event semantics change, update both the projection listener and any card status logic that depends on account state.

`ACCOUNT_CREATED` payload must include `currency`; CardService stores it in both
`cards.currency` and `account_ownership_projection.currency`.

If transaction completion or compensation semantics change, update card limit hold release behavior and the scheduler assumptions together.

`CardLimitReservationIT.compensationEventReleasesReservedHoldAndCardLimits` verifies the
PostgreSQL-backed compensation operation: a real reserved hold is compensated and both card spend
counters are restored. The Kafka listener is covered separately as a unit-level delegation.

## Kafka key strategy

| Topics | Order owner / outbox aggregateId |
| --- | --- |
| card.created, card.frozen, card.unfrozen | `cardId` |

The service outbox factory sets `eventKey = aggregateId.toString()`. Event type,
direction and event ID never participate in the key. Publishers send the stored key
without choosing a fixed partition, so different aggregates can use different partitions.
The persisted outbox row UUID is the unique event ID; publishers include it separately
in the `eventId` header for tracing and event-based deduplication. Retries reuse that ID.
Consumers must not deduplicate using the aggregate key.

For a fixed partition count, records with the same key in one topic use the same
partition and Kafka preserves their producer send order. The same key does **not**
provide ordering across different topics (including lifecycle event types currently
routed to separate topics), nor does it establish business order between concurrent
outbox publishers or retries. Increasing the partition count can remap a key.

Rollout: pause event creation, drain pending outbox records with the old publishers,
wait for all consumers to drain the old records, apply migrations, then deploy the
new factories, publishers and listeners together before resuming creation. Existing
outbox rows retain their stored keys; historical records are not rewritten. Across
the old/new key boundary, records may reside in different partitions.

Idempotent listeners require the `eventId` header and store it in the existing
`processed_events.event_key` column. Missing IDs fail message handling rather than
silently treating the aggregate key as an event identity. Replaying historical records
without this header requires an explicit stable event-ID backfill before replay.

## Outbox delivery

Publishing uses `support:0.0.4` claim/lease and guarded acknowledgements. Retries preserve
eventId and the stored Kafka key. Delivery is at least once; consumers must retain deduplication.
See [shared outbox publishing](../../docs/outbox.md) for settings, guarantees, metrics and rollout.
