# CardService Interfaces

[Docs Index](README.md)

## gRPC Service

Service implementation: `CardGrpcService`.

| Operation | Purpose |
| --- | --- |
| `getCardHealth` | Health check |
| `createCard` | Create a card for an account |
| `updateCard` | Update card details or limits |
| `getCardsByAccountId` | Read cards linked to an account |
| `reserveLimitsForTransaction` | Reserve daily and monthly card limit spend for a transaction |

`CardResponse` includes the configured limits and current spend counters: `dailyLimit`, `monthlyLimit`, `spendDailyLimit`, and `spendMonthlyLimit`.

## REST Exposure

Card REST endpoints are exposed through `ApiGateway/CardGatewayController`.

Gateway route groups include:

- `POST /card/create`
- `PUT /card/update`
- `GET /card/health`

## Contracts

gRPC types come from `com.burov:contracts`. Account and card event payloads come from `com.burov:kafka-contracts`.
