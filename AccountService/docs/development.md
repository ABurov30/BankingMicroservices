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

For private GitHub Packages from `BankingProtoContracts`, `BankKafkaContracts`, and `BankingSupport`:

```bash
GITHUB_TOKEN=replace_me ./mvnw -s .mvn/settings-docker.xml spotless:check checkstyle:check test
```

`.mvn/settings-docker.xml` defines separate Maven server ids for `github-proto-contracts`,
`github-kafka-contracts`, and `github-support`. The Dockerfile runs Maven with `-U` so container
builds refresh private package metadata instead of reusing stale cached artifacts.

## Common Change Areas

- Financial flows: add focused service tests.
- Schema changes: add Liquibase migrations.
- Event consumers: verify idempotency through `ProcessedEventEntity` and the `processedevent`
  helpers from `com.burov:support`.
- Event producers: update outbox payload mappers and downstream consumers.
