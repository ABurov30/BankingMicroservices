# ApiGateway Development

[Docs Index](README.md)

## Local Run

```bash
cp .env.example .env.local
make dev
```

The gateway expects domain services to be reachable on the gRPC hosts and ports configured in `.env.local`.

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

- Route changes: update the relevant controller and security rules.
- Authentication changes: update `SecurityConfig`, `CookieConfig`, and tests.
- gRPC mapping changes: update client, mapper classes, REST DTOs, and service docs together.
- Push notification changes: update Kafka listener, DTO mapper, and WebSocket behavior.
- WebSocket/STOMP changes: update `docs/asyncapi.yaml`, `docs/interfaces.md`, payload DTOs, authentication details, and WebSocket tests together.
- Runtime API docs: Swagger UI is available at `/swagger-ui.html`; AsyncAPI UI is available at `/asyncapi`.
