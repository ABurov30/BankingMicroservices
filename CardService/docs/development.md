# CardService Development

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

- Card fields: update entity, migrations, DTOs, and mappers.
- Card limit reservation: update `CardEntity`, `CardLimitHoldEntity`, repositories, schedulers, gRPC mapping, and docs together.
- Account projection behavior: update listener and idempotency logic backed by the
  `processedevent` helpers from `com.burov:support`.
- Event publication: update outbox payload mapper and notification consumers.
