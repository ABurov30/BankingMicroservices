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

## Outbox settings

The publisher uses the validated `banking.outbox.*` settings, including configurable polling,
initial delay, batch size, in-flight, lease and retry. Local/dev polling defaults to 500 ms.
See [shared outbox publishing](../../docs/outbox.md) for settings, guarantees, metrics and rollout.

## First administrator

Set these variables in `Infra/.env` for Compose, or in the environment / `AuthService/.env`
for a local run (`.env.local` and `.env.docker` must be explicitly loaded by the launcher):

| Variable | Default | Purpose |
| --- | --- | --- |
| `AUTH_BOOTSTRAP_ADMIN_ENABLED` | `false` | Enable startup seed after database migrations |
| `AUTH_BOOTSTRAP_ADMIN_EMAIL` | empty | Administrator login email, required when enabled |
| `AUTH_BOOTSTRAP_ADMIN_PASSWORD` | empty | Initial password, required when enabled; 12–72 characters and at most 72 UTF-8 bytes |

Generate a unique password and keep it only in ignored env files. Examples contain no password.
CI explicitly disables bootstrap. Log in through the existing login endpoint with these credentials.
The seed creates an `ACTIVE`, email-verified `ADMIN` with a BCrypt password hash in one transaction.
An existing ADMIN (including a blocked one) makes the seed a no-op: restarts never reset credentials
or unblock users. A configured email already owned by a non-admin fails startup without promotion.
Concurrent bootstrap instances lock the ADMIN role row before checking and creating the account.
Disable bootstrap after initial creation; changing env credentials does not change an existing account.

The seed creates a matching UserService profile through the `AUTH_USER_CREATED` Kafka event. It
does not emit a signup/verification email because this account is already email-verified. It has no
bank accounts or cards; use it for role management and administrative operations.
