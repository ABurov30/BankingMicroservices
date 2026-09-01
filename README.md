# Bank Microservices

Training banking microservices project built with Spring Boot. `ApiGateway` exposes the public REST API, domain services communicate through gRPC, and asynchronous integration uses Kafka with Avro contracts.

## Navigation

- [Agent instructions](AGENTS.md)
- [CI workflow](.github/workflows/ci.yml)
- [Infrastructure compose file](Infra/docker-compose.yml)

## Services

| Service | Purpose | HTTP | gRPC | Docs |
| --- | --- | --- | --- | --- |
| `ApiGateway` | REST entrypoint, JWT/cookie security, gRPC clients, WebSocket notifications | `8080` | - | [ApiGateway/docs/README.md](ApiGateway/docs/README.md) |
| `AuthService` | Signup, login, refresh/logout, roles, verification, JWT issuing | `8081` | `8090` | [AuthService/docs/README.md](AuthService/docs/README.md) |
| `UserService` | User profiles and reactions to auth events | `8082` | `8091` | [UserService/docs/README.md](UserService/docs/README.md) |
| `AccountService` | Accounts, balances, holds, freezes, and funds reservation | `8083` | `8092` | [AccountService/docs/README.md](AccountService/docs/README.md) |
| `CardService` | Cards, limits, spend counters, statuses, and account ownership projection | `8084` | `8093` | [CardService/docs/README.md](CardService/docs/README.md) |
| `TransactionService` | Transaction creation and card/account reservation flow | `8085` | `8094` | [TransactionService/docs/README.md](TransactionService/docs/README.md) |
| `NotificationService` | Email and push notifications from Kafka events | `8086` | `8095` | [NotificationService/docs/README.md](NotificationService/docs/README.md) |

`Infra` contains the shared Docker Compose setup, Kafka, Schema Registry, PostgreSQL, MongoDB, and development Helm manifests.

## Stack

- Java 17, Maven Wrapper
- Spring Boot 4.1.0
- Spring Security OAuth2 Resource Server in `ApiGateway`
- gRPC through `grpc-netty-shaded`
- Kafka, Avro, Confluent Schema Registry
- PostgreSQL 16, MongoDB 7
- Liquibase for PostgreSQL-backed services
- Spotless and Checkstyle
- Docker Compose

## Quick Start

GitHub Packages requires a token with `read:packages` access to the contracts, Kafka contracts, and support package repositories.

```bash
cd Infra
cp .env.example .env
mkdir -p secrets
openssl genrsa -out secrets/private.pem 2048
openssl rsa -in secrets/private.pem -pubout -out secrets/public.pem
docker compose up --build -d
```

After startup:

- API Gateway: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Kafka UI: `http://localhost:8079`
- Schema Registry: `http://localhost:9081`

Stop the environment:

```bash
cd Infra
docker compose down -v
```

## Checks

Run these commands from a service directory:

```bash
./mvnw spotless:check
./mvnw checkstyle:check
./mvnw test
```

For private Maven packages from `BankingProtoContracts`, `BankKafkaContracts`, and `BankingSupport`:

```bash
GITHUB_TOKEN=replace_me ./mvnw -s .mvn/settings-docker.xml spotless:check checkstyle:check test
```

CI runs formatting, Checkstyle, and tests for every service.
