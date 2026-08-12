# AuthService Overview

[Docs Index](README.md)

## Responsibility

`AuthService` handles signup, login, logout, refresh tokens, password changes, user verification, manager moderation flows, admin role changes, and JWT signing.

## Runtime Role

- Runs as `auth-service`.
- Exposes HTTP on `AUTH_PORT`, normally `8081`.
- Exposes gRPC on `AUTH_GRPC_PORT`, normally `8090`.
- Stores state in PostgreSQL.
- Publishes auth lifecycle events to Kafka.
- Uses RSA private key for signing JWTs and public key metadata for local validation needs.

## Important Packages

- `grpc` - gRPC server and exception interceptor.
- `service` - auth, token, and outbox publishing logic.
- `entity` - JPA entities for auth users, roles, refresh tokens, and outbox events.
- `repository` - Spring Data repositories.
- `mapper` - command, event payload, and gRPC mappers.
- `dto` - internal command/result records.

## Downstream Impact

JWT claims and auth events are consumed by `ApiGateway`, `UserService`, and `NotificationService`. Treat role, status, and token changes as cross-service contract changes.
