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
`authUserId`, `amountMinorUnits`, and `currency`.

## Consumers

`AccountService` and `NotificationService` are sensitive to transaction event semantics.

## Agent Notes

Kafka consumers treat stale status events for missing transactions or terminal statuses as no-ops: they log the current state and return instead of retrying the same event.

If transaction statuses or event payloads change, update AccountService compensation handling and NotificationService transaction notification handling.
