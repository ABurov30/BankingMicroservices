# ApiGateway Overview

[Docs Index](README.md)

## Responsibility

`ApiGateway` owns the public HTTP surface of the system. It translates REST requests into gRPC calls to domain services and centralizes authentication checks based on access tokens stored in cookies.

## Runtime Role

- Runs as `api-gateway`.
- Exposes HTTP on `API_GATEWAY_PORT`, normally `8080`.
- Does not expose a gRPC server.
- Does not own a database.
- Depends on all domain services being reachable through gRPC.
- Consumes push-notification Kafka events and sends them to connected WebSocket clients.

## Important Packages

- `controller` - public REST controllers.
- `client` - gRPC clients for domain services.
- `config` - security, cookie, JWT, gRPC client, OpenAPI, and WebSocket configuration.
- `mapper` - DTO and gRPC mapping code.
- `listener` - Kafka listener for push notification events.
- `websocket` - WebSocket principal and handshake support.

## Integration Boundaries

`ApiGateway` should not implement domain rules. Domain decisions belong to the service behind the matching gRPC API. Gateway code should stay focused on authentication, authorization, request mapping, response mapping, cookies, and transport-level concerns.
