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

## Contracts

gRPC types come from `com.burov:contracts` version `0.0.26-SNAPSHOT`. Event payloads come from
`com.burov:kafka-contracts`. Shared outbox and processed-event helpers come from
`com.burov:support` version `0.0.1`.
