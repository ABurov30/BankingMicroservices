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

## Produced Events

Card changes are published through `CardOutboxPublisher`.

Known produced event categories include:

- card created
- card frozen
- card unfrozen

## Consumers

`NotificationService` consumes card events for user notifications.

## Agent Notes

Kafka consumers treat stale account/card state and card-limit hold events as no-ops: they log the current state and return instead of retrying the same event.

If account event semantics change, update both the projection listener and any card status logic that depends on account state.

`ACCOUNT_CREATED` payload must include `currency`; CardService stores it in both
`cards.currency` and `account_ownership_projection.currency`.

If transaction completion or compensation semantics change, update card limit hold release behavior and the scheduler assumptions together.
