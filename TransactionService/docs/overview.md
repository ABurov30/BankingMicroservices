# TransactionService Overview

[Docs Index](README.md)

## Responsibility

`TransactionService` owns transaction records and transaction state transitions. It uses `AccountService` over gRPC to reserve funds before committing transaction state.

## Runtime Role

- Runs as `transaction-service`.
- Exposes HTTP on `TRANSACTION_PORT`, normally `8085`.
- Exposes gRPC on `TRANSACTION_GRPC_PORT`, normally `8094`.
- Stores transaction state in PostgreSQL.
- Calls `AccountService` over gRPC.
- Consumes transaction completion and compensation events.
- Publishes transaction events through an outbox.

## Important Packages

- `grpc` - transaction gRPC API implementation.
- `client` - `AccountGrpcClient`.
- `listener` - Kafka consumers for account transaction events.
- `service` - transaction and outbox logic.
- `entity` - transaction, processed event, and outbox entities.
- `repository` - database access.
- `mapper` - command, DTO, event payload, and gRPC mapping.

## Integration Boundaries

`TransactionService` should not mutate account balances directly. Funds movements are delegated to `AccountService`.
