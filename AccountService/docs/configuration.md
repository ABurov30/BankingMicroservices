# AccountService Configuration

[Docs Index](README.md)

## Config Loading

The service imports optional env-style properties files from the current directory and from `AccountService/.env`.

## Required Variables

| Variable | Purpose |
| --- | --- |
| `ACCOUNT_NAME` | Spring application name |
| `ACCOUNT_PORT` | HTTP port |
| `ACCOUNT_GRPC_PORT` | gRPC port |
| `ACCOUNT_SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL |
| `ACCOUNT_SPRING_DATASOURCE_USERNAME` | PostgreSQL username |
| `ACCOUNT_SPRING_DATASOURCE_PASSWORD` | PostgreSQL password |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap servers |
| `SCHEMA_REGISTRY_URL` | Confluent Schema Registry URL |

## Local Defaults

The service `.env.example` points local database access at `localhost:5434` and local Kafka at `localhost:29092`.

## Secrets

Do not commit real datasource passwords or GitHub package tokens.
