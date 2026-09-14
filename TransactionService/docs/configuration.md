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
No local Bulkhead exists in these services; circuit rejections are explicitly distinct from
Bulkhead saturation and remote RESOURCE_EXHAUSTED. Do not interpret the circuit rejection
counter as a concurrency saturation counter.

References: [Resilience4j CircuitBreaker](https://resilience4j.readme.io/docs/circuitbreaker),
[gRPC retry semantics](https://grpc.io/docs/guides/retry/).
