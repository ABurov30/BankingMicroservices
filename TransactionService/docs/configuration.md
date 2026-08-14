# TransactionService Configuration

[Docs Index](README.md)

## Config Loading

The service imports optional env-style properties files from the current directory and from `TransactionService/.env`.

## Required Variables

| Variable | Purpose |
| --- | --- |
| `TRANSACTION_NAME` | Spring application name |
| `TRANSACTION_PORT` | HTTP port |
| `TRANSACTION_GRPC_PORT` | gRPC port |
| `TRANSACTION_SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL |
| `TRANSACTION_SPRING_DATASOURCE_USERNAME` | PostgreSQL username |
| `TRANSACTION_SPRING_DATASOURCE_PASSWORD` | PostgreSQL password |
| `ACCOUNT_GRPC_HOST` | AccountService gRPC host |
| `ACCOUNT_GRPC_PORT` | AccountService gRPC port |
| `CARD_GRPC_HOST` | CardService gRPC host |
| `CARD_GRPC_PORT` | CardService gRPC port |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap servers |
| `SCHEMA_REGISTRY_URL` | Confluent Schema Registry URL |

## Local Defaults

The service `.env.example` points local database access at `localhost:5436` and local Kafka at `localhost:29092`. Transaction creation also needs reachable AccountService and CardService gRPC endpoints.

## Secrets

Do not commit real datasource passwords or GitHub package tokens.
