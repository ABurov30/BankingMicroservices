# TransactionService Development

[Docs Index](README.md)

## Local Run

```bash
cp .env.example .env.local
make dev
```

Local runtime needs PostgreSQL, Kafka, Schema Registry, and reachable `CardService` and `AccountService` gRPC endpoints.

For terminal-only restart on save, install `watchexec` and use:

```bash
brew install watchexec
make dev-hot
```

`dev-hot` runs the application and recompiles changed Java/resources files. Spring Boot DevTools
then restarts the application when the compiled classes in `target/classes` change.

## Checks

```bash
./mvnw spotless:check
./mvnw checkstyle:check
./mvnw test
```

For private GitHub Packages from `BankingProtoContracts`, `BankKafkaContracts`, and `BankingSupport`:

```bash
GITHUB_TOKEN=replace_me ./mvnw -s .mvn/settings-docker.xml spotless:check checkstyle:check test
```

`.mvn/settings-docker.xml` defines separate Maven server ids for `github-proto-contracts`,
`github-kafka-contracts`, and `github-support`. The Dockerfile runs Maven with `-U` so container
builds refresh private package metadata instead of reusing stale cached artifacts.

## Common Change Areas

- Transaction creation: verify CardService limit reservation and AccountService funds reservation flows.
- Transaction query behavior: update DTO and gRPC mappers.
- State transitions: update event payloads, listeners, and tests.
