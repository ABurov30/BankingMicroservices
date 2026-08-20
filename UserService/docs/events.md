# UserService Events

[Docs Index](README.md)

## Consumed Events

| Event | Source | Purpose |
| --- | --- | --- |
| `AUTH_USER_CREATED` | `AuthService` | Create profile projection |
| `AUTH_USER_BLOCKED` | `AuthService` | Block profile projection |
| `AUTH_USER_UNLOCK` | `AuthService` | Unblock profile projection |
| `AUTH_USER_VERIFIED` | `AuthService` | Mark profile as verified/active |
| `AUTH_USER_ROLE_CHANGED` | `AuthService` | Update projected role |
| `AUTH_SOCIAL_ACCOUNT_AUTH_USER_CREATED` | `AuthService` | Create profile projection for OAuth-created auth users |

## Produced Events

User profile changes are published through `UserOutboxPublisher`. Downstream consumers include `AccountService`.

Known produced event categories include:

- user profile created, including profiles created from OAuth/social auth users
- user profile blocked
- user profile unblocked or active again

## Agent Notes

Kafka consumers treat stale or already-applied auth state events as no-ops: they log the current state and return instead of throwing for retry.

If an auth event changes shape, update the listener, command mapper, and idempotency behavior together.
