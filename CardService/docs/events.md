# CardService Events

[Docs Index](README.md)

## Consumed Events

| Event | Source | Purpose |
| --- | --- | --- |
| `ACCOUNT_CREATED` | `AccountService` | Create or update account ownership projection |
| `ACCOUNT_FROZEN` | `AccountService` | Freeze related card availability |
| `ACCOUNT_UNFROZEN` | `AccountService` | Restore related card availability |

## Produced Events

Card changes are published through `CardOutboxPublisher`.

Known produced event categories include:

- card created
- card frozen
- card unfrozen

## Consumers

`NotificationService` consumes card events for user notifications.

## Agent Notes

If account event semantics change, update both the projection listener and any card status logic that depends on account state.
