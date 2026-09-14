# TransactionService Events

[Docs Index](README.md)

## Consumed Events

| Event | Source | Purpose |
| --- | --- | --- |
| `TRANSACTION_COMPLETED` | `AccountService` | Mark transaction as completed |
| `TRANSACTION_COMPENSATED` | `AccountService` | Mark transaction as compensated or failed after account flow |

## Produced Events

Transaction state changes are published through `TransactionOutboxPublisher`.

Known produced event categories include:

- transaction created or started
- transaction failed

`TRANSACTION_FAILED` uses `TransactionFailedEventPayload` from `kafka-contracts`; payload contains
`authUserId`, `amountMinorUnits`, and `currency`. Currency mismatch during card-limit or
account-funds reservation fails the transaction and emits this event.

## Consumers

`AccountService` and `NotificationService` are sensitive to transaction event semantics.

## Agent Notes

Kafka consumers treat stale status events for missing transactions or terminal statuses as no-ops: they log the current state and return instead of retrying the same event.

If transaction statuses or event payloads change, update AccountService compensation handling and NotificationService transaction notification handling.

## Kafka key strategy

| Topics | Order owner / outbox aggregateId |
| --- | --- |
| transaction.funds.requested, transaction.failed | `transactionId` |

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
