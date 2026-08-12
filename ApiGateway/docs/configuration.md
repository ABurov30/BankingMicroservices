# ApiGateway Configuration

[Docs Index](README.md)

## Config Loading

`application.properties` imports an optional env-style properties file:

```properties
spring.config.import=optional:file:./${ENV_FILE:.env.local}[.properties],optional:file:./ApiGateway/${ENV_FILE:.env.local}[.properties]
```

The default local file is `.env.local`. Set `ENV_FILE` to use a different file.

## Required Variables

| Variable | Purpose |
| --- | --- |
| `API_GATEWAY_NAME` | Spring application name |
| `API_GATEWAY_PORT` | HTTP port |
| `JWT_PUBLIC_KEY_PATH` | Public key used to verify access tokens |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap servers |
| `SCHEMA_REGISTRY_URL` | Confluent Schema Registry URL |
| `AUTH_GRPC_HOST`, `AUTH_GRPC_PORT` | Auth gRPC target |
| `USER_GRPC_HOST`, `USER_GRPC_PORT` | User gRPC target |
| `ACCOUNT_GRPC_HOST`, `ACCOUNT_GRPC_PORT` | Account gRPC target |
| `CARD_GRPC_HOST`, `CARD_GRPC_PORT` | Card gRPC target |
| `TRANSACTION_GRPC_HOST`, `TRANSACTION_GRPC_PORT` | Transaction gRPC target |
| `NOTIFICATION_GRPC_HOST`, `NOTIFICATION_GRPC_PORT` | Notification gRPC target |

## Secrets

The gateway needs only the JWT public key. It must not receive the private key.

In Docker Compose, `Infra/secrets/public.pem` is mounted read-only into the gateway container.
