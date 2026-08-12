# CardService Overview

[Docs Index](README.md)

## Responsibility

`CardService` owns card lifecycle state. It creates cards, updates cards, reads cards by account, and reacts to account lifecycle events.

## Runtime Role

- Runs as `card-service`.
- Exposes HTTP on `CARD_PORT`, normally `8084`.
- Exposes gRPC on `CARD_GRPC_PORT`, normally `8093`.
- Stores card and account ownership projection state in PostgreSQL.
- Consumes account events.
- Publishes card events through an outbox.

## Important Packages

- `grpc` - card gRPC API implementation.
- `listener` - Kafka consumers for account events.
- `service` - card business logic and outbox publishing.
- `entity` - card, account ownership projection, processed event, and outbox entities.
- `repository` - database access.
- `mapper` - command, result, event payload, and gRPC mapping.

## Integration Boundaries

`CardService` should not own account balances. It consumes account events to maintain enough account ownership data for card operations.
