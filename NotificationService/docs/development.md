# NotificationService Development

[Docs Index](README.md)

## Local Run

```bash
cp .env.example .env.local
make dev
```

Local runtime needs PostgreSQL, MongoDB, Kafka, Schema Registry, and SMTP settings.

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

- New notification type: update listener, resolver, payload DTO, mapper, and tests.
- Email template changes: verify template variables and `EmailNotificationTemplateResolver`.
- Push payload changes: verify `ApiGateway/GatewayKafkaListener`.
- Persistence changes: update Liquibase migrations or Mongo document mapping as needed.
