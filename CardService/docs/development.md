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

## Unit and Integration Tests

Surefire includes `**/Test*.java`, `**/*Test.java`, `**/*Tests.java`,
`**/*TestCase.java`, and `**/*IT.java`. The `IT` suffix enables discovery;
exclusion from the default run requires `@Tag("integration")`.

`./mvnw test` and `./mvnw clean verify` run unit tests by default.
Tests requiring the Spring application context or external infrastructure must use
JUnit `@Tag("integration")`; Surefire excludes that tag by default.
`CardServiceApplicationTests.contextLoads` belongs to this group.

Spring context tests use `@ActiveProfiles("test")` to load test profile settings.
External dependencies and any remaining environment variables are still required.

Run unit and integration tests together with:

```bash
./mvnw clean verify -Pintegration-tests
```

This Maven profile enables the integration tag; it does not configure Spring properties
or start infrastructure. Supply the service environment and start its dependencies first.
For a shell-compatible `.env.local`, run from the service directory:

```bash
(
  set -a
  source ./.env.local
  set +a
  ./mvnw clean verify -Pintegration-tests
)
```

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
No minimum coverage threshold is enforced. The default run executes unit tests; integration
tests require the `integration-tests` Maven profile.

## Card Limit Reservation Tests

`CardLimitReservationIT` uses a disposable PostgreSQL 16 Testcontainers database and the real
Liquibase migrations. Docker must be running; Kafka and the service's shared database are not
needed for this JPA slice test. Run only these integration tests with:

```bash
./mvnw -s .mvn/settings-docker.xml -Pintegration-tests -Dtest=CardLimitReservationIT test
```

The tests call the Spring-managed `CardService` and `CardLimitReservationService` beans with
independent transactions. A barrier after the hold existence query forces concurrent requests
to overlap before the card lock is acquired. They verify daily and monthly limit contention,
concurrent duplicate transaction IDs, and the persisted hold and both counters. A deferred
PostgreSQL trigger injects a failure at commit to verify rollback of both writes and the
`FAILED` response from the outer service. The trigger exists only in the disposable test database.

## Common Change Areas

- Card fields: update entity, migrations, DTOs, and mappers.
- Card limit reservation: update `CardEntity`, `CardLimitHoldEntity`, repositories, schedulers, gRPC mapping, and docs together.
- Account projection behavior: update listener and idempotency logic backed by the
  `processedevent` helpers from `com.burov:support`.
- Event publication: update outbox payload mapper and notification consumers.
