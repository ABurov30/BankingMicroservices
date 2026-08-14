# TransactionService Development

[Docs Index](README.md)

## Local Run

```bash
cp .env.example .env.local
make dev
```

Local runtime needs PostgreSQL, Kafka, Schema Registry, and reachable `CardService` and `AccountService` gRPC endpoints.

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

- Transaction creation: verify CardService limit reservation and AccountService funds reservation flows.
- Transaction query behavior: update DTO and gRPC mappers.
- State transitions: update event payloads, listeners, and tests.
