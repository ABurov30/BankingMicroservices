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

For GitHub Packages:

```bash
GITHUB_TOKEN=replace_me ./mvnw -s .mvn/settings-docker.xml test
```

## Common Change Areas

- Password or token behavior: update `AuthService`, `TokenService`, repositories, and tests.
- Role behavior: update role mapping and gateway authorization expectations.
- Persistence changes: add Liquibase migrations.
- Event changes: update outbox payload mapping and downstream consumers.
