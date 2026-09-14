# Outbox publishing

Account, Auth, Card, Transaction, User and PushNotification publishers use
`com.burov:support:0.0.4` (`JpaOutboxAttemptStore` and `OutboxDispatcher`).
Each service provides its entity/table, payload mapper, Kafka message headers,
configuration adapter and database backlog metrics.

## Configuration

All six services expose the same settings. Millisecond settings accept integer values.
They are adapted into the library's validated `OutboxProperties` bean, named
`outboxProperties`. The schedulers use its values through `fixedDelayString` and
`initialDelayString`; changes take effect after application restart.
Do not configure the library's separate `outbox.*` prefix in these applications.

| Property | Environment variable | Default |
| --- | --- | --- |
| `banking.outbox.poll-interval-ms` | `BANKING_OUTBOX_POLL_INTERVAL_MS` | 5000; local/dev and development Compose: 500 |
| `banking.outbox.initial-delay-ms` | `BANKING_OUTBOX_INITIAL_DELAY_MS` | 5000 |
| `banking.outbox.batch-size` | `BANKING_OUTBOX_BATCH_SIZE` | 50 |
| `banking.outbox.max-in-flight` | `BANKING_OUTBOX_MAX_IN_FLIGHT` | 50 per publisher/replica |
| `banking.outbox.lease-timeout-ms` | `BANKING_OUTBOX_LEASE_TIMEOUT_MS` | 360000 |
| `banking.outbox.retry-delay-ms` | `BANKING_OUTBOX_RETRY_DELAY_MS` | 5000 |
| `banking.outbox.max-attempts` | `BANKING_OUTBOX_MAX_ATTEMPTS` | 5 |

Activate Spring profile `local` or `dev`, or use the service `.env.example`, for
500 ms polling. `Infra/docker-compose.yml` passes the settings to all six publishers
through a shared environment anchor; its default is also 500 ms.
The base 5000 ms setting is a conservative fallback, not a measured production recommendation.
All durations and counts must be positive, except initial delay may be zero.

Kafka producer limits are configured explicitly:

| Kafka setting | Environment variable | Default |
| --- | --- | --- |
| `max.block.ms` | `KAFKA_PRODUCER_MAX_BLOCK_MS` | 5000 |
| `delivery.timeout.ms` | `KAFKA_PRODUCER_DELIVERY_TIMEOUT_MS` | 30000 |
| `request.timeout.ms` | `KAFKA_PRODUCER_REQUEST_TIMEOUT_MS` | 10000 |

Startup validates the effective producer factory configuration against:

```text
lease-timeout-ms > min(batch-size, max-in-flight) * max.block.ms
                   + delivery.timeout.ms + 5000
```

The budget covers sequential producer admission of a claimed batch, broker delivery
and a 5-second margin. Increasing batch size or producer timeouts may require a larger
lease. Payload mapping, Schema Registry access, DB delays and process pauses are not
strictly bounded by these Kafka settings; monitor them and keep additional headroom.

## Claim, send and recovery

1. Each poll recovers at most `batch-size` expired attempts using row locks and
   `SKIP LOCKED`. Exhausted legacy PENDING rows are also marked FAILED.
2. In a short `REQUIRES_NEW` transaction, the store selects PENDING rows with attempts
   remaining and due `next_retry_at`, ordered by `created_at, id`, using
   `FOR UPDATE SKIP LOCKED`. The claim size is capped by available in-flight slots.
3. Claim increments `retry_count`, assigns PROCESSING, database `locked_at` and
   a unique UUID attempt token in `locked_by`. Entities are detached before send.
4. Dispatch suspends any caller transaction. The service maps its payload and checks
   that the attempt token still owns an unexpired DB lease immediately before Kafka send.
5. The actual Kafka acknowledgement future drives completion in a separate transaction,
   conditional on event ID, PROCESSING and attempt token. Success becomes PUBLISHED;
   an error becomes PENDING with `next_retry_at`, or FAILED at the attempt limit.
   `sent_at` is set on success; ownership fields are cleared on completion.
6. Expired attempts return to PENDING with a retry delay, or FAILED at the limit.
   Old callbacks cannot update a record whose token has changed or been cleared.

`retry_count` counts claimed attempts, including process crashes. FAILED records are
terminal and require an explicit operational decision to replay. Recovery is delayed
by the lease, the retry delay and a subsequent poll. The lease default deliberately
prioritizes avoiding premature recovery over fast failover.

Local in-flight slots remain occupied until the acknowledgement/completion callback
finishes, even after DB lease expiry. A never-completing producer future can exhaust
that replica's slots; investigate the producer and restart the replica if necessary.
Other replicas can recover its expired DB attempts. Failed DB completion is logged
and recovered through the same lease mechanism.

## Delivery guarantees

Concurrent replicas cannot claim the same currently owned row. A normal outstanding
Kafka acknowledgement keeps it PROCESSING. Attempt tokens fence database completion;
the extra pre-send check rejects attempts already known to be expired or superseded.

Delivery remains **at least once**. There is no atomic transaction between PostgreSQL
and Kafka. A crash after broker acceptance but before DB completion can produce a
retry. A process paused after its ownership check may resume after another replica
has recovered the lease; the check does not fence that old Kafka producer. Absolute
send exclusion across such pauses requires broker-side fencing, which this integration
does not implement. Consumers must continue deduplicating by the stable `eventId`.

Topics, payload schemas, stored aggregate Kafka keys, eventId headers and Account's
`transaction-notification-direction` header are preserved. Multiple replicas and
retries do not establish business ordering per aggregate.

## Metrics and production sizing

All metrics have an `outbox` tag containing the table name; application tags distinguish
services. Library metrics:

- `outbox.claimed`: claimed attempts.
- `outbox.recovered`: recovered expired attempts and exhausted PENDING rows.
- `outbox.completed{outcome=published|failed|stale}`: guarded callback outcomes;
  `failed` includes retriable failures, not only terminal FAILED records.
- `outbox.callback.errors`: DB completion failures.
- `outbox.in.flight`: occupied local dispatch slots.
- `outbox.ack.duration`: time from sender invocation through callback completion.

Service metrics:

- `outbox.pending` and `outbox.processing`: current DB row counts, including delayed retries.
- `outbox.retry`: send attempts with `retry_count > 1`.
- `outbox.publication.latency`: event creation to successful broker ack, including polling
  and retry time. This measures broker acknowledgement, not DB completion. Keep replica
  clocks/timezones aligned with event creation timestamps.

Backlog gauges execute count queries on scrape. Replicas sharing a table report the
same backlog: use `max` per application/outbox, not a sum over replicas. Counters and
in-flight values are replica-local and can be summed. Library completion/latency meters
appear after their first observation.

To choose a production interval, test representative sustained load and bursts with
the intended replica count. Compare 5000, 1000 and 500 ms while observing publication
latency, backlog age/size, DB query rate/CPU/connections, callback errors, retries and
in-flight saturation. Lower the interval only while latency improves without growing
DB contention or recovery/retry rates. Increase batch/concurrency only within DB and
Kafka capacity, then recheck the lease budget. Record the chosen value and workload
in the deployment configuration; no production measurements are available in this change.

For percentile measurements, enable a histogram, for example:

```properties
management.metrics.distribution.percentiles-histogram.outbox.publication.latency=true
```

## Validation

`AccountService/OutboxConcurrencyIT` runs against a Testcontainers PostgreSQL 16 database
with real service Liquibase migrations. It covers competing publisher instances,
disjoint batches, SKIP LOCKED, delayed retry, terminal failure, lease recovery, stale
callbacks, in-flight limits, detached entities, transaction suspension and pre-send
ownership checks. Service configuration tests cover overrides and invalid settings.

```bash
cd AccountService
./mvnw -s .mvn/settings-docker.xml -Pintegration-tests -Dtest=OutboxConcurrencyIT test
KAFKA_BOOTSTRAP_SERVERS=localhost:29092 ./mvnw -s .mvn/settings-docker.xml \
  -Pintegration-tests -Dtest=OutboxKafkaKeyIT test
```

The Kafka key test uses a real broker and a mocked attempt store. It verifies routing,
headers and order within one publisher batch; the PostgreSQL test verifies ownership.

## Rollout and rollback

1. Make `support:0.0.4` available to all service builds and complete checks.
2. Stop all old publisher schedulers/replicas and let outstanding sends and callbacks
   finish. Prevent old versions from restarting during the transition.
3. Apply each service's `*-outbox-processing.sql` Liquibase migration. Existing fields
   are reused; migrations expand status CHECK and add partial pending/lease indexes.
   Index creation and ALTER TABLE can block writes: plan a maintenance window based
   on table size. Migrations do not rewrite stored keys or event IDs.
4. Start the new replicas with the same intended configuration. Do not mix old and new
   publishers: old versions ignore claims. Observe backlog, success, failure and recovery.
5. Before rollback, stop and drain new publishers and resolve all PROCESSING attempts.
   Do not make a row PENDING while its send can still complete. Expanding status support
   and leaving indexes in place is compatible with old code only after PROCESSING is empty;
   returning to old publishers also removes the concurrency protection.
