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

## Kafka key strategy

| Topics | Order owner / outbox aggregateId |
| --- | --- |
| user.profile.created, user.profile.blocked, user.profile.UNLOCK | `userProfileId` |

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
