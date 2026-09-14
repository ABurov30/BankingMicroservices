# gRPC Bulkhead load check

[Configuration](configuration.md#grpc-bulkhead) | [Docs index](README.md)

## Reproduce

From ApiGateway, with private Maven packages available:

```bash
./mvnw -s .mvn/settings-docker.xml -Dtest=GrpcBulkheadLoadTest -Dgrpc.bulkhead.load=true test
```

The opt-in test writes `target/bulkhead-load.csv`. It uses actual gRPC in-process transport,
blocking clients and the production interceptor/registries. It needs no database or running bank
services. Ordinary unit runs skip this load test. CircuitBreaker and Bulkhead are active; retry
is disabled to isolate concurrent admission.

## Scenario and results

Local run on 2026-09-14: 64 caller threads, three simultaneous bursts of 64 requests for each
limit (192 requests), downstream worker pool of 8 with 200 ms processing per RPC, deadline 2 s.
A second independent, fast Card dependency is called through the same caller pool during each
Account burst. Values include JVM scheduling, instrumentation and rejection logging overhead.
The downstream worker queue models overload; production Bulkhead admission has no queue.

| Limit | Requests | Success | Rejected | Peak client | Peak downstream | Success p95 ms | Rejection p95 ms | Healthy max ms |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 4 | 192 | 12 | 180 | 4 | 4 | 265.72 | 39.36 | 29.72 |
| 8 | 192 | 24 | 168 | 8 | 8 | 311.94 | 105.89 | 62.87 |
| 16 | 192 | 48 | 144 | 16 | 8 | 524.47 | 91.22 | 79.09 |
| 32 | 192 | 96 | 96 | 32 | 8 | 950.22 | 74.35 | 43.99 |


All runs respected the configured client concurrency and restored all permits. Card calls
completed while Account was saturated. A limit of 4 underutilized the modeled downstream;
8 filled its worker pool. Raising admission to 16 or 32 increased queued work and successful-call
latency without increasing its eight-worker processing capacity. Eight is therefore the provisional
unary default for this development model. Burst rejection percentages are intentional and are
not measurements of sustainable production throughput. Stream default 32 is separately provisional.

## Limits of this result and production sizing

No bank containers were running locally during this check. This is an isolation/load experiment,
not a measurement of real AccountService/CardService capacity, Tomcat or database contention.
The repository does not explicitly configure caller/downstream executor sizes or DB pool limits.
Do not extrapolate the modeled eight-worker capacity to the real services.

Before production rollout:

1. Record actual request/gRPC executor sizes, downstream DB pool sizes, CPU, workload mix,
   gateway/transaction replica counts and downstream replica counts.
2. Reserve caller threads for healthy dependencies and non-gRPC work. Account for all simultaneous
   outgoing limits, not just one. For overlapping calls, consider request fan-out and async execution.
3. Measure safe downstream throughput and latency with realistic reads, reservations and writes;
   use an isolated test dataset with idempotency keys. Estimate concurrency from throughput ×
   mean RPC duration, add measured headroom and check p95/p99 under bursts and slow responses.
4. Budget aggregate incoming concurrency: all Gateway replicas plus TransactionService replicas
   consume Account/Card capacity. Per-replica limits alone do not enforce a global budget.
5. Sweep `grpc.resilience.instances.account.bulkhead.max-concurrent-calls` and the Card override,
   monitoring rejection rate, latency, busy caller threads, downstream CPU and DB wait time.
   Choose the smallest limits that meet the healthy-load SLO while retaining outage isolation.

The unit tests in both services separately cover error/deadline/cancellation release, dependency
isolation, stream lifetime, HALF_OPEN permission return, zero-wait validation, Prometheus meters
and permit retention throughout native retry backoff.
