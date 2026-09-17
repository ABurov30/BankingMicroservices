# TransactionService Interfaces

[Docs Index](README.md)

## gRPC Service

Service implementation: `TransactionGrpcService`.

| Operation | Purpose |
| --- | --- |
| `getTransactionHealth` | Health check |
| `createTransaction` | Create a transaction, reserve card limits, start funds reservation, and return the created transaction summary |
| `getTransactionsByAccounts` | Read transactions for account ids |
| `watchTransactionStatus` | Stream status updates for one transaction after verifying that the subscribing auth user owns the source or target account |

`watchTransactionStatus` returns `TransactionStatusResponse` messages with `minorUnits`, `currency`,
`status`, and non-sensitive `sourceAccount` / `targetAccount` data.

## REST Exposure

Transaction REST endpoints are exposed through `ApiGateway/TransactionGatewayController`.

Gateway route groups include:

- `POST /transaction/creat-transaction`
- `GET /transaction/user/{userId}`
- `GET /transaction/health`

The route name `creat-transaction` is currently spelled that way in code.

`POST /transaction/creat-transaction` requires `sourceCardId` in addition to source account, target account, `minorUnits`, currency, and idempotency key. The response includes `transactionId`, `minorUnits`, `currency`, and `status`.

## External gRPC Calls

`TransactionService` calls `CardService` through `CardGrpcClient` for card limit reservation, then
calls `AccountService` through `AccountGrpcClient` for funds reservation. Both reservation requests
carry the transaction `currency`; downstream services reject the reservation when it does not match
the source card or source account currency.

Transaction response enrichment and status streaming use the internal AccountService operation
`getAccountByIdsForTransaction`. The ownership-protected `getAccountById` operation is reserved for
end-user account reads that include authenticated user identity and role.

## Contracts

gRPC types come from `com.burov:contracts` version `0.0.29`. Event payloads come from
`com.burov:kafka-contracts`. Shared outbox and processed-event helpers come from
`com.burov:support` version `0.0.7`.

Source and target accounts for subscription authorization and each status update are fetched
with one batch RPC and matched by account ID, independent of response order. Duplicate IDs
are sent once; missing accounts fail with NOT_FOUND. Subscription access still requires the
caller to own either account. Public response schemas and stream destinations are unchanged.

## Dependency Failures

Outbound gRPC circuits reject calls with UNAVAILABLE while OPEN or when HALF_OPEN probes are
fully occupied. TransactionService preserves downstream gRPC statuses for its callers.
ApiGateway returns HTTP 503 for UNAVAILABLE, DEADLINE_EXCEEDED, INTERNAL and RESOURCE_EXHAUSTED,
using the existing `ApiErrorResponse` shape. Business status mappings remain unchanged.
There is no successful fallback for reads or writes. Unary read retries are bounded by the
existing deadline; writes are not automatically replayed. See [configuration](configuration.md#grpc-resilience).

Local gRPC Bulkhead saturation returns RESOURCE_EXHAUSTED (`Downstream bulkhead saturated:
<dependency>-grpc`), mapped to HTTP 503 at ApiGateway. This differs from the UNAVAILABLE
returned by an OPEN circuit. Neither local rejection starts or retries a downstream RPC.
