# TransactionService Overview

[Docs Index](README.md)

## Responsibility

`TransactionService` owns transaction records and coordinates the reservation Saga. It uses
`CardService` over gRPC to reserve card limits, persists the successful card reservation locally,
then calls `AccountService` to reserve funds.

## Runtime Role

- Runs as `transaction-service`.
- Exposes HTTP on `TRANSACTION_PORT`, normally `8085`.
- Exposes gRPC on `TRANSACTION_GRPC_PORT`, normally `8094`.
- Stores transaction state in PostgreSQL.
- Calls `CardService` and `AccountService` over gRPC.
- Consumes transaction completion and compensation events.
- Publishes transaction events through an outbox.

## Important Packages

- `grpc` - transaction gRPC API implementation.
- `client` - `AccountGrpcClient` and `CardGrpcClient`.
- `listener` - Kafka consumers for account transaction events.
- `service` - transaction and outbox logic.
- `entity` - transaction, processed event, and outbox entities.
- `repository` - database access.
- `mapper` - command, DTO, event payload, and gRPC mapping.

## Integration Boundaries

`TransactionService` should not mutate account balances or card spend counters directly. Funds movements are delegated to `AccountService`; card limit reservations are delegated to `CardService`.

## Reservation Saga

`CREATED` is persisted before any remote call. After CardService succeeds,
`CARD_LIMIT_RESERVED` is committed in its own local transaction before the AccountService gRPC
call. A successful account reservation advances the transaction to `FUNDS_RESERVED`, followed by
`FUNDS_REQUESTED` when the transfer request is added to the outbox.

If a reservation fails, TransactionService atomically stores `FAILED`, the error message,
`TRANSACTION_FAILED`, and `TRANSACTION_CARD_LIMIT_HOLD_COMPENSATION` in one local transaction.
The compensation event releases the idempotent card hold. Database transactions never span gRPC
calls.
