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

For private GitHub Packages from `BankingProtoContracts`, `BankKafkaContracts`, and `BankingSupport`:

```bash
GITHUB_TOKEN=replace_me ./mvnw -s .mvn/settings-docker.xml spotless:check checkstyle:check test
```

`.mvn/settings-docker.xml` defines separate Maven server ids for `github-proto-contracts`,
`github-kafka-contracts`, and `github-support`. The Dockerfile runs Maven with `-U` so container
builds refresh private package metadata instead of reusing stale cached artifacts.

## Unit and Integration Tests

Surefire includes `**/Test*.java`, `**/*Test.java`, `**/*Tests.java`,
`**/*TestCase.java`, and `**/*IT.java`. The `IT` suffix enables discovery;
exclusion from the default run requires `@Tag("integration")`.

`./mvnw test` and `./mvnw clean verify` run unit tests by default.
Tests requiring the Spring application context or external infrastructure must use
JUnit `@Tag("integration")`; Surefire excludes that tag by default.
`UserServiceApplicationTests.contextLoads` belongs to this group.

Spring context tests use `@ActiveProfiles("test")` to load test profile settings.
Each integration test class starts PostgreSQL with Testcontainers and supplies connection
settings through `@ServiceConnection`. Docker must be available locally and in CI.
The context startup test disables Kafka listeners and uses placeholder Kafka and
Schema Registry addresses; it verifies application wiring, not messaging connectivity.

Run unit and integration tests together with:

```bash
./mvnw clean verify -Pintegration-tests
```

This Maven profile enables the integration tag. No prestarted PostgreSQL, Kafka,
Schema Registry, or service environment file is required for these tests.

CI enables this profile for every service to keep checking application startup.

## Test Coverage

JaCoCo 0.8.15 collects test coverage automatically. Run from this service directory:

```bash
./mvnw clean verify
```

For private packages, export `GITHUB_TOKEN` and run:

```bash
./mvnw -s .mvn/settings-docker.xml clean verify
```

Open `target/site/jacoco/index.html` in a browser. `./mvnw test` collects coverage
in `target/jacoco.exec`; the report is generated during `verify`.
No minimum coverage threshold is enforced.

## Common Change Areas

- Profile fields: update entity, migrations, DTOs, mappers, and gRPC responses.
- Event consumption: update listener, command mapper, idempotency annotation usage, the
  `processedevent` support integration, and tests.
- Outbox publication: update event payload mapper and downstream consumers.
