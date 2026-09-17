# NotificationService Events

[Docs Index](README.md)

## Consumed Events

| Event | Source | Purpose |
| --- | --- | --- |
| `AUTH_USER_CREATED` | `AuthService` | Create signup notifications |
| `AUTH_USER_BLOCKED` | `AuthService` | Notify blocked users |
| `AUTH_USER_UNLOCK` | `AuthService` | Notify unlocked users |
| `AUTH_USER_VERIFIED` | `AuthService` | Notify verified users |
| `AUTH_USER_FORGET_PASSWORD` | `AuthService` | Create password reset notification with the reset token in the email URL query parameter |
| `ACCOUNT_CREATED` | `AccountService` | Notify account creation |
| `ACCOUNT_FROZEN` | `AccountService` | Notify account freeze |
| `ACCOUNT_UNFROZEN` | `AccountService` | Notify account unfreeze |
| `CARD_CREATED` | `CardService` | Notify card creation |
| `CARD_FROZEN` | `CardService` | Notify card freeze |
| `CARD_UNFROZEN` | `CardService` | Notify card unfreeze |
| `TRANSACTION_FAILED` | `TransactionService` | Notify transaction failure |
| `TRANSACTION_COMPLETED` | `AccountService` | Notify transaction completion |

`TRANSACTION_FAILED` payloads do not include an account number; failed transaction push
notifications include the failed `amountMinorUnits` rendered with its `currency`.

## Produced Events

| Event | Consumer | Purpose |
| --- | --- | --- |
| `PUSH_NOTIFICATION_CREATED` | `ApiGateway` | Deliver push notification over WebSocket |

## Agent Notes

When adding a new notification type, update the listener, resolver, command mapper, template if email is involved, and push payload mapping.

## Kafka key strategy

| Topics | Order owner / outbox aggregateId |
| --- | --- |
| notification.push.created | `authUserId (recipient)` |

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

`authUserId` is the available stable user identity in this service; `userProfileId`
is not available in these event creation flows.
The push outbox aggregate is now the recipient (`AUTH_USER`), rather than the
individual notification document. Notification document IDs remain independent.

Idempotent listeners require the `eventId` header and store it in the existing
`processed_events.event_key` column. Missing IDs fail message handling rather than
silently treating the aggregate key as an event identity. Replaying historical records
without this header requires an explicit stable event-ID backfill before replay.

## Outbox delivery

Publishing uses `support:0.0.7` claim/lease and guarded acknowledgements. Retries preserve
eventId and the stored Kafka key. Delivery is at least once; consumers must retain deduplication.
See [shared outbox publishing](../../docs/outbox.md) for settings, guarantees, metrics and rollout.
