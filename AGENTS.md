# AGENTS.md

Instructions for agents working in this repository.

## Before Changing Code

- Read [README.md](README.md) and the relevant service documentation.
- Each service is a separate Maven project with its own `pom.xml`, `.mvn/settings-docker.xml`, Dockerfile, `.env.example`, `README.md`, `AGENTS.md`, and `docs` folder.
- Do not commit real `.env` files, JWT keys, SMTP passwords, or GitHub tokens.
- If you change build, formatting, or Checkstyle behavior, verify all seven services.

## Quality Commands

Run from a service directory:

```bash
./mvnw spotless:check
./mvnw checkstyle:check
./mvnw test
```

For private packages:

```bash
GITHUB_TOKEN=replace_me ./mvnw -s .mvn/settings-docker.xml spotless:check checkstyle:check test
```

## Service Documents

| Service | Docs | Local README | Local AGENTS |
| --- | --- | --- | --- |
| `ApiGateway` | [ApiGateway/docs/README.md](ApiGateway/docs/README.md) | [ApiGateway/README.md](ApiGateway/README.md) | [ApiGateway/AGENTS.md](ApiGateway/AGENTS.md) |
| `AuthService` | [AuthService/docs/README.md](AuthService/docs/README.md) | [AuthService/README.md](AuthService/README.md) | [AuthService/AGENTS.md](AuthService/AGENTS.md) |
| `UserService` | [UserService/docs/README.md](UserService/docs/README.md) | [UserService/README.md](UserService/README.md) | [UserService/AGENTS.md](UserService/AGENTS.md) |
| `AccountService` | [AccountService/docs/README.md](AccountService/docs/README.md) | [AccountService/README.md](AccountService/README.md) | [AccountService/AGENTS.md](AccountService/AGENTS.md) |
| `CardService` | [CardService/docs/README.md](CardService/docs/README.md) | [CardService/README.md](CardService/README.md) | [CardService/AGENTS.md](CardService/AGENTS.md) |
| `TransactionService` | [TransactionService/docs/README.md](TransactionService/docs/README.md) | [TransactionService/README.md](TransactionService/README.md) | [TransactionService/AGENTS.md](TransactionService/AGENTS.md) |
| `NotificationService` | [NotificationService/docs/README.md](NotificationService/docs/README.md) | [NotificationService/README.md](NotificationService/README.md) | [NotificationService/AGENTS.md](NotificationService/AGENTS.md) |

## Editing Rules

- Follow the service's existing package patterns: `controller`, `grpc`, `service`, `repository`, `mapper`, `listener`, `dto`, `entity`.
- Do not edit generated gRPC contracts inside services; contracts come from `com.burov:contracts`.
- Use Kafka event types from `com.burov:kafka-contracts`.
- Add new database migrations under `src/main/resources/db/changelog/changes` and include them from `db.changelog-master.yaml`.
- After formatting, verify `spotless:check` and `checkstyle:check` in every touched service.
