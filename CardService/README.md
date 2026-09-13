# CardService

Service documentation: [docs/README.md](docs/README.md).

Project overview: [../README.md](../README.md).
Agent instructions: [AGENTS.md](AGENTS.md).

Unit tests and coverage: run `./mvnw clean verify` and open `target/site/jacoco/index.html`.
See [development instructions](docs/development.md#test-coverage) for private package access.

Run unit and integration tests with `./mvnw clean verify -Pintegration-tests`
after configuring the service environment and starting its dependencies.

Surefire discovers the standard test names and classes ending in `IT`.
Use `@Tag("integration")` to exclude integration tests from the default run.

Spring context tests activate the `test` profile with `@ActiveProfiles("test")`.
