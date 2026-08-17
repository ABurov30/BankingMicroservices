# ApiGateway Data and State

[Docs Index](README.md)

## Persistence

`ApiGateway` does not own persistent storage. It should not write domain state directly.

## Request State

Runtime request state is derived from:

- JWT access token cookie `at`.
- Refresh token cookie `rt` for auth refresh/logout flows.
- JWT claims such as roles and user status.
- WebSocket handshake principal data.

Auth cookies are HTTP-only, secure by default, and use path `/`. Cookie `Domain`, `SameSite`, and
`Secure` attributes are configured through `AUTH_COOKIE_DOMAIN`, `AUTH_COOKIE_SAME_SITE`, and
`AUTH_COOKIE_SECURE`. Logout clears cookies with the same scope attributes used when they were set.

## Domain Data Flow

Domain data is fetched through gRPC clients and mapped to REST DTOs. If a new REST endpoint needs domain data, add or reuse a gRPC call instead of introducing a gateway-side repository.

## Generated and Derived Data

The gateway may create API response DTOs and WebSocket notification DTOs. Those objects should remain transport-level data and should not become a gateway-owned domain model.
