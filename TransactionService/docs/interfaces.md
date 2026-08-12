# TransactionService Interfaces

[Docs Index](README.md)

## gRPC Service

Service implementation: `TransactionGrpcService`.

| Operation | Purpose |
| --- | --- |
| `getTransactionHealth` | Health check |
| `createTransaction` | Create a transaction and start funds reservation |
| `getTransactionsByAccounts` | Read transactions for account ids |

## REST Exposure

Transaction REST endpoints are exposed through `ApiGateway/TransactionGatewayController`.

Gateway route groups include:

- `POST /transaction/creat-transaction`
- `GET /transaction/user/{userId}`
- `GET /transaction/health`

The route name `creat-transaction` is currently spelled that way in code.

## External gRPC Calls

`TransactionService` calls `AccountService` through `AccountGrpcClient` for funds reservation.

## Contracts

gRPC types come from `com.burov:contracts`. Event payloads come from `com.burov:kafka-contracts`.
