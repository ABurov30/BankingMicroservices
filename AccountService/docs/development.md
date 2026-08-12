# AccountService Development

[Docs Index](README.md)

## Local Run

```bash
cp .env.example .env.local
make dev
```

Local runtime needs PostgreSQL, Kafka, and Schema Registry.

## Checks

```bash
./mvnw spotless:check
./mvnw checkstyle:check
./mvnw test
```

For GitHub Packages:

```bash
GITHUB_TOKEN=replace_me ./mvnw -s .mvn/settings-docker.xml test
```

## Common Change Areas

- Financial flows: add focused service tests.
- Schema changes: add Liquibase migrations.
- Event consumers: verify idempotency through `ProcessedEventEntity`.
- Event producers: update outbox payload mappers and downstream consumers.
