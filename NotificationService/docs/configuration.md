# NotificationService Configuration

[Docs Index](README.md)

## Config Loading

The service imports optional env-style properties files from the current directory and from `NotificationService/.env`.

## Required Variables

| Variable | Purpose |
| --- | --- |
| `NOTIFICATION_NAME` | Spring application name |
| `NOTIFICATION_PORT` | HTTP port |
| `NOTIFICATION_GRPC_PORT` | gRPC port |
| `SITE_URL` | Site URL used in user-facing notifications |
| `NOTIFICATION_SPRING_DATASOURCE_URL` | PostgreSQL JDBC URL |
| `NOTIFICATION_SPRING_DATASOURCE_USERNAME` | PostgreSQL username |
| `NOTIFICATION_SPRING_DATASOURCE_PASSWORD` | PostgreSQL password |
| `NOTIFICATION_MONGO_URI` | MongoDB connection URI |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap servers |
| `SCHEMA_REGISTRY_URL` | Confluent Schema Registry URL |
| `MAIL_HOST` | SMTP host |
| `MAIL_PORT` | SMTP port |
| `MAIL_USERNAME` | SMTP username |
| `MAIL_PASSWORD` | SMTP password |

## Local Defaults

The service `.env.example` points local MongoDB access at `localhost:27017` and local Kafka at `localhost:29092`.

## Secrets

Do not commit real datasource passwords, MongoDB passwords, SMTP credentials, or GitHub package tokens.

## Outbox settings

The publisher uses the validated `banking.outbox.*` settings, including configurable polling,
initial delay, batch size, in-flight, lease and retry. Local/dev polling defaults to 500 ms.
See [shared outbox publishing](../../docs/outbox.md) for settings, guarantees, metrics and rollout.
