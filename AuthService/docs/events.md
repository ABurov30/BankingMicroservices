# AuthService Events

[Docs Index](README.md)

## Produced Events

Auth lifecycle changes are written to `AuthOutboxEventEntity` and published by `AuthOutboxPublisher`.

Known auth event categories include:

- auth user created
- auth user blocked
- auth user unlocked
- auth user verified
- auth user role changed
- auth user password reset or forget-password flow events
- social account auth user created

## Consumed Events

The service has Kafka consumer configuration with group id `auth-service`. Current business logic primarily uses auth-owned state and produced events.

## Consumers

Downstream consumers include:

- `UserService` for user profile projection changes.
- `NotificationService` for email and push notifications.
- `ApiGateway` indirectly through JWT claims and auth gRPC responses.

## Agent Notes

When changing event payloads or topics, update the contracts package first and then update every consumer.

## Kafka key strategy

| Topics | Order owner / outbox aggregateId |
| --- | --- |
| auth.user.created, auth.user.blocked, auth.user.unlock, auth.user.verified, auth.user.role.changed, auth.user.forget.password, auth.social.account.auth.user.created | `authUserId` |

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

Idempotent listeners require the `eventId` header and store it in the existing
`processed_events.event_key` column. Missing IDs fail message handling rather than
silently treating the aggregate key as an event identity. Replaying historical records
without this header requires an explicit stable event-ID backfill before replay.

## Outbox delivery

Publishing uses `support:0.0.4` claim/lease and guarded acknowledgements. Retries preserve
eventId and the stored Kafka key. Delivery is at least once; consumers must retain deduplication.
See [shared outbox publishing](../../docs/outbox.md) for settings, guarantees, metrics and rollout.
