# TransactionService Development

[Docs Index](README.md)

## Local Run

```bash
cp .env.example .env.local
make dev
```

Local runtime needs PostgreSQL, Kafka, Schema Registry, and a reachable `AccountService` gRPC endpoint.

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

- Transaction creation: verify AccountService reserve flow.
- Transaction query behavior: update DTO and gRPC mappers.
- State transitions: update event payloads, listeners, and tests.
