# AccountService Overview

[Docs Index](README.md)

## Responsibility

`AccountService` owns bank account state. It creates accounts, reads accounts, freezes and unfreezes accounts, updates balances, and reserves funds for transactions.

## Runtime Role

- Runs as `account-service`.
- Exposes HTTP on `ACCOUNT_PORT`, normally `8083`.
- Exposes gRPC on `ACCOUNT_GRPC_PORT`, normally `8092`.
- Stores account state in PostgreSQL.
- Consumes user profile and transaction events.
- Publishes account and transaction-related events through an outbox.

## Important Packages

- `grpc` - account gRPC API implementation.
- `listener` - Kafka consumers for user and transaction events.
- `service` - account, transfer, currency, scheduler, and outbox logic.
- `entity` - account, hold, currency, processed event, and outbox entities.
- `repository` - database access.
- `mapper` - command, result, event payload, and gRPC mapping.

## Integration Boundaries

`AccountService` owns account balances and should be the only service that mutates account funds. Transaction orchestration should call it over gRPC instead of updating account data directly.
