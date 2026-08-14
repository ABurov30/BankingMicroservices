# NotificationService Events

[Docs Index](README.md)

## Consumed Events

| Event | Source | Purpose |
| --- | --- | --- |
| `AUTH_USER_CREATED` | `AuthService` | Create signup notifications |
| `AUTH_USER_BLOCKED` | `AuthService` | Notify blocked users |
| `AUTH_USER_UNLOCK` | `AuthService` | Notify unlocked users |
| `AUTH_USER_VERIFIED` | `AuthService` | Notify verified users |
| `AUTH_USER_FORGET_PASSWORD` | `AuthService` | Create password reset notification |
| `ACCOUNT_CREATED` | `AccountService` | Notify account creation |
| `ACCOUNT_FROZEN` | `AccountService` | Notify account freeze |
| `ACCOUNT_UNFROZEN` | `AccountService` | Notify account unfreeze |
| `CARD_CREATED` | `CardService` | Notify card creation |
| `CARD_FROZEN` | `CardService` | Notify card freeze |
| `CARD_UNFROZEN` | `CardService` | Notify card unfreeze |
| `TRANSACTION_FAILED` | `TransactionService` | Notify transaction failure |
| `TRANSACTION_COMPLETED` | `AccountService` | Notify transaction completion |

`TRANSACTION_FAILED` payloads do not include an account number; failed transaction push notifications only include the failed amount.

## Produced Events

| Event | Consumer | Purpose |
| --- | --- | --- |
| `PUSH_NOTIFICATION_CREATED` | `ApiGateway` | Deliver push notification over WebSocket |

## Agent Notes

When adding a new notification type, update the listener, resolver, command mapper, template if email is involved, and push payload mapping.
