# UserService Development

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

- Profile fields: update entity, migrations, DTOs, mappers, and gRPC responses.
- Event consumption: update listener, command mapper, idempotency annotation usage, and tests.
- Outbox publication: update event payload mapper and downstream consumers.
