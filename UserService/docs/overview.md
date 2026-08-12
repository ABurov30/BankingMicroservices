# UserService Overview

[Docs Index](README.md)

## Responsibility

`UserService` owns the user profile projection used by the banking domain. It reacts to auth-domain lifecycle events and exposes profile reads over gRPC.

## Runtime Role

- Runs as `user-service`.
- Exposes HTTP on `USER_PORT`, normally `8082`.
- Exposes gRPC on `USER_GRPC_PORT`, normally `8091`.
- Stores user profile state in PostgreSQL.
- Consumes auth events.
- Publishes user profile events through an outbox.

## Important Packages

- `grpc` - user gRPC API implementation.
- `listener` - Kafka consumers for auth events.
- `service` - profile business logic and outbox publishing.
- `entity` - user profile, processed event, and outbox entities.
- `repository` - database access.
- `mapper` - event, command, result, and gRPC mapping.

## Integration Boundaries

`UserService` should not validate credentials or issue tokens. Those responsibilities belong to `AuthService`. It should focus on profile state and profile query semantics.
