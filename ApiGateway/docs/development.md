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

For GitHub Packages:

```bash
GITHUB_TOKEN=replace_me ./mvnw -s .mvn/settings-docker.xml test
```

## Common Change Areas

- Route changes: update the relevant controller and security rules.
- Authentication changes: update `SecurityConfig`, `CookieConfig`, and tests.
- gRPC mapping changes: update client and mapper classes together.
- Push notification changes: update Kafka listener, DTO mapper, and WebSocket behavior.
