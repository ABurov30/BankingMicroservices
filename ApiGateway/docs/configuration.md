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
| `SITE_URL` | UI URL used for OAuth2 success redirects and CORS allowed origin |
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
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | Google OAuth2 client credentials |

## Optional Variables

| Variable | Default | Purpose |
| --- | --- | --- |
| `AUTH_COOKIE_DOMAIN` | empty | Optional `Domain` attribute for auth cookies |
| `AUTH_COOKIE_SAME_SITE` | `Lax` | `SameSite` attribute for auth cookies |
| `AUTH_COOKIE_SECURE` | `true` | `Secure` attribute for auth cookies |

## Cookie Scope

Auth cookies are issued by `ApiGateway`, so browsers scope them to the gateway host by
default. To share cookies between the UI and gateway on the same parent domain, set
`AUTH_COOKIE_DOMAIN` to that parent domain, for example `buro-bank.ru`.

Do not include a scheme or port in `AUTH_COOKIE_DOMAIN`. Browsers cannot accept cookies for an
unrelated domain. For cross-site deployments, use `AUTH_COOKIE_SAME_SITE=None` with
`AUTH_COOKIE_SECURE=true` and send frontend requests with credentials.

## Secrets

The gateway needs only the JWT public key. It must not receive the private key.

In Docker Compose, `Infra/secrets/public.pem` is mounted read-only into the gateway container.
