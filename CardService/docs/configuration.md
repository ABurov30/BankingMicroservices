# CardService Configuration

[Docs Index](README.md)

## Config Loading

The service imports optional env-style properties files from the current directory and from `CardService/.env`.

## Required Variables

| Variable | Purpose |
| --- | --- |
| `CARD_NAME` | Spring application name |
| `CARD_PORT` | HTTP port |
| `CARD_GRPC_PORT` | gRPC port |
| `CARD_SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL |
| `CARD_SPRING_DATASOURCE_USERNAME` | PostgreSQL username |
| `CARD_SPRING_DATASOURCE_PASSWORD` | PostgreSQL password |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap servers |
| `SCHEMA_REGISTRY_URL` | Confluent Schema Registry URL |

## Local Defaults

The service `.env.example` points local database access at `localhost:5435` and local Kafka at `localhost:29092`.

## Secrets

Do not commit real datasource passwords or GitHub package tokens.
