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

## Consumers

`AccountService` and `NotificationService` are sensitive to transaction event semantics.

## Agent Notes

If transaction statuses or event payloads change, update AccountService compensation handling and NotificationService transaction notification handling.
