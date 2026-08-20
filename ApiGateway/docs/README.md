# ApiGateway Docs

[Project README](../../README.md) | [Service README](../README.md) | [Service AGENTS](../AGENTS.md)

`ApiGateway` is the public entrypoint for the banking system. It exposes REST endpoints, validates JWT access tokens from cookies, calls domain services over gRPC, and forwards push notifications over WebSocket.

## Documents

- [Overview](overview.md)
- [Interfaces](interfaces.md)
- [AsyncAPI WebSocket Contract](asyncapi.yaml)
- [Configuration](configuration.md)
- [Data and State](data.md)
- [Events](events.md)
- [Development](development.md)

## Runtime Documentation

- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/v3/api-docs`
- AsyncAPI UI: `/asyncapi` or `/asyncapi-ui.html`
- AsyncAPI YAML: `/asyncapi.yaml`
