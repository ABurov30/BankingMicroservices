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

## Consumers

Downstream consumers include `CardService`, `TransactionService`, and `NotificationService`.

## Agent Notes

Kafka consumers treat stale funds-transfer events for missing or non-reserved holds as no-ops: they log the current state and return instead of retrying the same event.

When changing funds reservation or compensation events, update `TransactionService` and `NotificationService` expectations.
