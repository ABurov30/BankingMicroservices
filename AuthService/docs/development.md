# AuthService Development

[Docs Index](README.md)

## Local Run

```bash
cp .env.example .env.local
make dev
```

Local runtime needs PostgreSQL, Kafka, Schema Registry, and JWT keys.

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

- Password or token behavior: update `AuthService`, `TokenService`, repositories, and tests.
- Role behavior: update role mapping and gateway authorization expectations.
- Persistence changes: add Liquibase migrations.
- Event changes: update outbox payload mapping and downstream consumers.
