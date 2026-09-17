# AccountService Events

[Docs Index](README.md)

## Consumed Events

| Event | Source | Purpose |
| --- | --- | --- |
| `USER_PROFILE_CREATED` | `UserService` | Create initial account-related state for a user |
| `USER_PROFILE_BLOCKED` | `UserService` | Freeze or restrict user accounts |
| user profile unlock events | `UserService` | Restore account availability where allowed |
| transaction failure events | `TransactionService` | Compensate or release reserved funds |

## Produced Events

Account and transaction-funds changes are published through the account outbox.

`ACCOUNT_CREATED` payload contains `accountId`, `authUserId`, `accountNumber`, and `currency`.

Known produced event categories include:

- account created
- account frozen
- account unfrozen
- transaction completed
- transaction compensated
- account hold released by time (contains `transactionId`; emitted transactionally by the expiry scheduler)
- cache invalidation

## Cache Invalidation

When an account-overview cache entry becomes stale, AccountService writes a `CACHE_INVALIDATION`
event to the account outbox in the same transaction as the account change. Account creation,
status changes, balance top-ups and withdrawals, reservations, transfers, compensations, expired
holds, and savings-interest accrual all invalidate the owner's overview. The payload contains
`keys`, a list of shared cache keys generated through `CacheKeyGenerator`.

After the outbox publisher sends the event to Kafka, ApiGateway deletes those keys from Redis L2
and publishes them to Redis Pub/Sub. Every live Gateway instance then removes the matching local
Caffeine L1 entry. Cache-key generation is shared through the support library so producers and
the Gateway use the same key format.

## Consumers

Downstream consumers include `CardService`, `TransactionService`, and `NotificationService`.

## Agent Notes

Kafka consumers treat stale funds-transfer events for missing or non-reserved holds as no-ops: they log the current state and return instead of retrying the same event.

When changing funds reservation or compensation events, update `TransactionService` and `NotificationService` expectations.

## Kafka key strategy

| Topics | Order owner / outbox aggregateId |
| --- | --- |
| account.created, account.frozen, account.unfrozen | `accountId` |
| transaction.completed, transaction.compensated | `transactionId` |

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

Both sender and recipient `TRANSACTION_COMPLETED` records, and compensation records,
use the saga `transactionId`. Direction remains in the payload and the existing
`transaction-notification-direction` header. Each record retains its own event ID.

`OutboxKafkaKeyIT` exercises the real publisher against Kafka with a temporary four-partition
topic, verifies per-aggregate send order and distinct event IDs, and checks that multiple
partitions are used. It uses JSON serialization of Avro records to avoid requiring Schema
Registry; this test covers key routing and headers, not Avro wire compatibility. Run:

```bash
KAFKA_BOOTSTRAP_SERVERS=localhost:29092 ./mvnw -Pintegration-tests -Dtest=OutboxKafkaKeyIT test
```

Idempotent listeners require the `eventId` header and store it in the existing
`processed_events.event_key` column. Missing IDs fail message handling rather than
silently treating the aggregate key as an event identity. Replaying historical records
without this header requires an explicit stable event-ID backfill before replay.

## Outbox delivery

Publishing uses `support:0.0.7` claim/lease and guarded acknowledgements. Retries preserve
eventId and the stored Kafka key. Delivery is at least once; consumers must retain deduplication.
See [shared outbox publishing](../../docs/outbox.md) for settings, guarantees, metrics and rollout.
