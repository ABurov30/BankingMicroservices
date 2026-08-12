# AuthService Configuration

[Docs Index](README.md)

## Config Loading

The service imports optional env-style properties files from the current directory and from `AuthService/.env`.

## Required Variables

| Variable | Purpose |
| --- | --- |
| `AUTH_NAME` | Spring application name |
| `AUTH_PORT` | HTTP port |
| `AUTH_GRPC_PORT` | gRPC port |
| `AUTH_SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL |
| `AUTH_SPRING_DATASOURCE_USERNAME` | PostgreSQL username |
| `AUTH_SPRING_DATASOURCE_PASSWORD` | PostgreSQL password |
| `JWT_PRIVATE_KEY_PATH` | RSA private key path |
| `JWT_PUBLIC_KEY_PATH` | RSA public key path |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap servers |
| `SCHEMA_REGISTRY_URL` | Confluent Schema Registry URL |
| `SITE_URL` | URL used in user-facing auth flows |

## JWT Settings

The application sets:

- issuer: `auth-service`
- audience: `bank-api`
- access token TTL: 15 minutes
- refresh token TTL: 30 days

## Local Secrets

Do not commit real keys or tokens. For local Docker Compose, JWT keys normally live under `Infra/secrets`.
