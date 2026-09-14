# UserService

Service documentation: [docs/README.md](docs/README.md).

Project overview: [../README.md](../README.md).
Agent instructions: [AGENTS.md](AGENTS.md).

Unit tests and coverage: run `./mvnw clean verify` and open `target/site/jacoco/index.html`.
See [development instructions](docs/development.md#test-coverage) for private package access.

Run unit and integration tests with `./mvnw clean verify -Pintegration-tests`
with Docker running; Testcontainers starts a temporary PostgreSQL database.

Surefire discovers the standard test names and classes ending in `IT`.
Use `@Tag("integration")` to exclude integration tests from the default run.

Spring context tests activate the `test` profile with `@ActiveProfiles("test")`.
Database connection settings are supplied by Testcontainers. The context startup test
disables Kafka listeners and does not require Kafka or Schema Registry.

Kafka partition ownership, event IDs and rollout: [key strategy](docs/events.md#kafka-key-strategy).

Outbox claim/lease, configuration, metrics and rollout: [shared publishing guide](../docs/outbox.md).
