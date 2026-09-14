# TransactionService Configuration

[Docs Index](README.md)

## Config Loading

The service imports optional env-style properties files from the current directory and from `TransactionService/.env`.

## Required Variables

| Variable | Purpose |
| --- | --- |
| `TRANSACTION_NAME` | Spring application name |
| `TRANSACTION_PORT` | HTTP port |
| `TRANSACTION_GRPC_PORT` | gRPC port |
| `TRANSACTION_SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL |
| `TRANSACTION_SPRING_DATASOURCE_USERNAME` | PostgreSQL username |
| `TRANSACTION_SPRING_DATASOURCE_PASSWORD` | PostgreSQL password |
| `ACCOUNT_GRPC_HOST` | AccountService gRPC host |
| `ACCOUNT_GRPC_PORT` | AccountService gRPC port |
| `CARD_GRPC_HOST` | CardService gRPC host |
| `CARD_GRPC_PORT` | CardService gRPC port |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap servers |
| `SCHEMA_REGISTRY_URL` | Confluent Schema Registry URL |

## Local Defaults

The service `.env.example` points local database access at `localhost:5436` and local Kafka at `localhost:29092`. Transaction creation also needs reachable AccountService and CardService gRPC endpoints.

## Secrets

Do not commit real datasource passwords or GitHub package tokens.

## gRPC Resilience

Each outbound channel has one shared Resilience4j 2.3.0 CircuitBreaker; blocking, future and
streaming stubs share it. Instance names are `auth`, `user`, `account`, `card`, `transaction`,
`notification` in ApiGateway and `account`, `card` in TransactionService. Instances are local to
one service process and dependency; they are not created per request.

Defaults live under `grpc.resilience.defaults`. Override any setting per dependency using
`grpc.resilience.instances.account.<setting>` (ordinary Spring properties/environment binding).
Configuration changes require reload/restart; recovery from dependency outages does not.

| Setting | Default | Meaning |
| --- | --- | --- |
| `window-type` | `COUNT_BASED` | `COUNT_BASED` or `TIME_BASED` |
| `window-size` | `20` | Calls, or seconds for TIME_BASED |
| `minimum-calls` | `10` | Completed non-ignored calls before evaluating thresholds |
| `failure-rate-threshold` | `50` | Failure percentage |
| `slow-call-rate-threshold` | `50` | Slow-call percentage |
| `slow-call-duration-ms` | `1500` | Slow logical RPC threshold, including retries/backoff |
| `open-wait-ms` | `10000` | Time before the next request may enter HALF_OPEN |
| `half-open-calls` | `3` | Maximum admitted logical probes per HALF_OPEN evaluation |
| `half-open-max-wait-ms` | `5000` | Return to OPEN if probes never complete |
| `record-resource-exhausted` | `false` | Count remote RESOURCE_EXHAUSTED as a technical failure |
| `retry.max-attempts` | `2` | Total attempts, including initial; 1 disables policy; maximum 5 |
| `retry.initial-backoff-ms` | `100` | Initial native gRPC retry backoff |
| `retry.max-backoff-ms` | `500` | Maximum backoff |
| `retry.backoff-multiplier` | `2` | Exponential backoff multiplier; gRPC adds ±20% jitter |

UNAVAILABLE, DEADLINE_EXCEEDED and INTERNAL count as failures. All other error statuses are
ignored by the breaker unless RESOURCE_EXHAUSTED is enabled. INVALID_ARGUMENT, NOT_FOUND and
PERMISSION_DENIED therefore neither open the breaker nor dilute its failure/slow-call window.
Local circuit rejection never invokes the transport and never counts as a downstream failure.
After the open wait, requests trigger HALF_OPEN; successful probes close it without restart.

Retry uses the native gRPC service config, with an explicit allowlist of existing unary reads
(including health and batch reads) in `GrpcResilience.RETRY_READS`. Only UNAVAILABLE retries.
Writes, reservations, authentication commands, new unknown methods and status streams have no
application retry policy. Native transparent retries may still occur when gRPC knows the server
application did not process a call. No synthetic successful fallback or empty-result fallback is added.
The existing two-second unary deadline bounds all attempts and backoff together; it is not reset.
Breaker accounting wraps native retry: one logical result per RPC. HALF_OPEN permits at most
`half-open-calls` logical probes, each with at most `retry.max-attempts` transport attempts.
A status stream records availability on its first message; later messages/disconnects do not
inflate counts or treat the connection lifetime as a slow RPC. Streams are never replayed.

Actuator `/actuator/prometheus` exports `resilience4j_circuitbreaker_state`,
`resilience4j_circuitbreaker_calls_seconds_count{kind="failed"}` and
`resilience4j_circuitbreaker_not_permitted_calls_total`, tagged by instance `name`.
`grpc_client_circuit_transitions_total` additionally has `dependency` and `transition` labels.
Logs use `grpc_circuit_transition` and `grpc_circuit_rejected` with dependency/state.
Local Bulkhead rejection uses `grpc_bulkhead_rejected`, separate from circuit rejection and
remote RESOURCE_EXHAUSTED. Local saturation bypasses breaker outcome accounting even when
`record-resource-exhausted=true`.

References: [Resilience4j CircuitBreaker](https://resilience4j.readme.io/docs/circuitbreaker),
[gRPC retry semantics](https://grpc.io/docs/guides/retry/).


## gRPC Bulkhead

The same channel interceptor also acquires a Resilience4j semaphore Bulkhead permit. Each
replica shares one instance per dependency: `account-grpc`, `card-grpc`, and in ApiGateway
also `auth-grpc`, `user-grpc`, `transaction-grpc`, `notification-grpc`. All blocking/future unary
stubs targeting a dependency share its limit. No extra executor or admission queue is created.
These are per-process limits, not a distributed global downstream limit.

Use the existing `grpc.resilience.defaults` and `grpc.resilience.instances.<dependency>` prefixes:

| Setting | Default | Meaning |
| --- | --- | --- |
| `bulkhead.max-concurrent-calls` | `8` | Concurrent logical unary RPCs per dependency/replica |
| `bulkhead.max-wait-duration-ms` | `0` | Strict fail-fast admission; nonzero fails startup validation |
| `bulkhead.stream.max-concurrent-calls` | `32` | Active streams in a separate `<dependency>-grpc-stream` instance |
| `bulkhead.stream.max-wait-duration-ms` | `0` | Streams also require immediate admission |

For example, `grpc.resilience.instances.account.bulkhead.max-concurrent-calls=4` overrides
only Account calls. Permit waiting is deliberately prohibited because `ClientCall.start()`
also runs on async callers. Increasing concurrency is supported; enabling a waiting queue is not.

Admission order is CircuitBreaker permission → Bulkhead permission → native gRPC call/retry.
An OPEN circuit consumes no Bulkhead permit. Saturation returns RESOURCE_EXHAUSTED and returns
any reserved HALF_OPEN permission without recording a breaker outcome. It never reaches the
native retry layer. At ApiGateway this becomes HTTP 503 with the existing ApiErrorResponse.
Remote RESOURCE_EXHAUSTED retains the existing optional circuit failure accounting.

A unary permit covers the entire logical RPC, including retry/backoff. It is returned exactly
once on terminal onClose (success, error, deadline exceeded, or cancellation), and on synchronous
start failure. The original deadline is passed unchanged; Bulkhead does not extend or replace it.
A stream permit remains held after the first message until terminal close/cancellation, while
CircuitBreaker records stream availability on the first message. This prevents long-lived streams
from consuming unary permits. No fallback or automatic stream replay is introduced.

Prometheus exports, with `name` identifying the Bulkhead instance:

- `resilience4j_bulkhead_available_concurrent_calls`
- `resilience4j_bulkhead_max_allowed_concurrent_calls`
- `grpc_client_bulkhead_rejected_total`

For occupied permits, subtract available from max. Rejected counters are initialized to zero on
instance creation. Use these metrics with latency, caller busy threads and downstream DB/executor
queues. A local limit does not cap requests originating from other replicas or services.

The default 8 is a provisional development value. The reproducible synthetic load sweep and
capacity-sizing procedure are in [the load report](../../ApiGateway/docs/bulkhead-load.md).
Production tuning requires the real downstream workload, replica counts and thread/DB pool sizes.

## Outbox settings

The publisher uses the validated `banking.outbox.*` settings, including configurable polling,
initial delay, batch size, in-flight, lease and retry. Local/dev polling defaults to 500 ms.
See [shared outbox publishing](../../docs/outbox.md) for settings, guarantees, metrics and rollout.
