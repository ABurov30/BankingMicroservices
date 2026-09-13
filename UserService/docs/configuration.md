# UserService Configuration

[Docs Index](README.md)

## Config Loading

The service imports optional env-style properties files from the current directory and from `UserService/.env`.

## Required Variables

| Variable | Purpose |
| --- | --- |
| `USER_NAME` | Spring application name |
| `USER_PORT` | HTTP port |
| `USER_GRPC_PORT` | gRPC port |
| `USER_SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL |
| `USER_SPRING_DATASOURCE_USERNAME` | PostgreSQL username |
| `USER_SPRING_DATASOURCE_PASSWORD` | PostgreSQL password |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap servers |
| `SCHEMA_REGISTRY_URL` | Confluent Schema Registry URL |

## Local Defaults

The service `.env.example` points local database access at `localhost:5433` and local Kafka at `localhost:29092`.

Spring context tests activate the `test` profile. Testcontainers supplies PostgreSQL
connection settings through `@ServiceConnection`; Docker must be available.
The context startup test disables Kafka listeners and supplies placeholder Kafka and
Schema Registry addresses; it does not verify messaging connectivity.

## Secrets

Do not commit real datasource passwords or GitHub package tokens.
