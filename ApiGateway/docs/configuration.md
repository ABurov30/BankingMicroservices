# ApiGateway Configuration

[Docs Index](README.md)

## Config Loading

`application.properties` imports an optional env-style properties file:

```properties
spring.config.import=optional:file:./${ENV_FILE:.env.local}[.properties],optional:file:./ApiGateway/${ENV_FILE:.env.local}[.properties]
```

The default local file is `.env.local`. Set `ENV_FILE` to use a different file.

## Required Variables

| Variable | Purpose |
| --- | --- |
| `SITE_URL` | UI URL used for OAuth2 success redirects and CORS allowed origin |
| `API_GATEWAY_NAME` | Spring application name |
| `API_GATEWAY_PORT` | HTTP port |
| `JWT_PUBLIC_KEY_PATH` | Public key used to verify access tokens |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap servers |
| `SCHEMA_REGISTRY_URL` | Confluent Schema Registry URL |
| `AUTH_GRPC_HOST`, `AUTH_GRPC_PORT` | Auth gRPC target |
| `USER_GRPC_HOST`, `USER_GRPC_PORT` | User gRPC target |
| `ACCOUNT_GRPC_HOST`, `ACCOUNT_GRPC_PORT` | Account gRPC target |
| `CARD_GRPC_HOST`, `CARD_GRPC_PORT` | Card gRPC target |
| `TRANSACTION_GRPC_HOST`, `TRANSACTION_GRPC_PORT` | Transaction gRPC target |
| `NOTIFICATION_GRPC_HOST`, `NOTIFICATION_GRPC_PORT` | Notification gRPC target |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Google OAuth2 client credentials |

## Optional Variables

| Variable | Default | Purpose |
| --- | --- | --- |
| `AUTH_COOKIE_DOMAIN` | empty | Optional `Domain` attribute for auth cookies |
| `AUTH_COOKIE_SAME_SITE` | `Strict` | `SameSite` attribute for JWT auth cookies |
| `AUTH_COOKIE_SECURE` | `true` | `Secure` attribute for auth cookies |
| `ACCOUNT_OVERVIEW_CACHE_ENABLED` | `true` | Enables the account-overview cache |
| `ACCOUNT_OVERVIEW_CACHE_L1_TTL` | `2s` | L1 cache entry lifetime |
| `ACCOUNT_OVERVIEW_CACHE_L2_TTL` | `5s` | L2 cache entry lifetime |
| `ACCOUNT_OVERVIEW_CACHE_L1_MAX_SIZE` | `10000` | Maximum number of entries retained in L1 |

## Cookie Scope

Auth cookies are issued by `ApiGateway`, so browsers scope them to the gateway host by
default. To share cookies between the UI and gateway on the same parent domain, set
`AUTH_COOKIE_DOMAIN` to that parent domain, for example `buro-bank.ru`.

Do not include a scheme or port in `AUTH_COOKIE_DOMAIN`. Browsers cannot accept cookies for an
unrelated domain.

## Cookie and CSRF Policy

The gateway stores access (`at`) and refresh (`rt`) JWTs in cookies with `Path=/`,
`HttpOnly=true`, `Secure=true`, and `SameSite=Strict` by default. `HttpOnly` keeps tokens out of
browser JavaScript, `Secure` requires HTTPS, and `Strict` prevents the browser from automatically
sending these cookies on cross-site requests.

Google OAuth2 remains compatible with this policy. The OAuth authorization request is correlated
through the server session; its session cookie uses `SameSite=Lax` (`server.servlet.session.cookie.same-site`),
which allows Google's top-level `GET` redirect to `/login/oauth2/code/google`. The gateway issues
the JWT cookies only after that callback has successfully completed, so they may remain `Strict`.

`AUTH_COOKIE_SAME_SITE=None` must not be enabled until CSRF-token protection is implemented and
verified for every state-changing endpoint. `Secure=true` is mandatory for `SameSite=None`, but it
does not itself protect against CSRF. Prefer keeping the UI and gateway same-site so that the
default `Strict` policy can be retained.

The CORS policy accepts credentialed requests only from the exact origin derived from `SITE_URL`.
It is a browser access-control policy, not a replacement for the cookie and CSRF policy. Do not
introduce state-changing `GET` endpoints: mutations must use `POST`, `PUT`, `PATCH`, or `DELETE`.

## Secrets

The gateway needs only the JWT public key. It must not receive the private key.

In Docker Compose, `Infra/secrets/public.pem` is mounted read-only into the gateway container.

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
