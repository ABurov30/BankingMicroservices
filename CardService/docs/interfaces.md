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

`createCard` receives `currency` from ApiGateway or account-created events. The currency is the
source account currency and is persisted on the card.

`CardResponse` includes card currency, configured limits, and current spend counters in minor units.
The gRPC contract fields are `dailyLimitMinorUnits`, `monthlyLimitMinorUnits`,
`spendDailyLimitMinorUnits`, `spendMonthlyLimitMinorUnits`, and `currency`.

`reserveLimitsForTransaction` receives `minorUnits` and `currency` from `TransactionService`.
The requested currency must match the source card currency. CardService applies the requested minor
units directly to card limit checks, holds, and spend counters.

## REST Exposure

Card REST endpoints are exposed through `ApiGateway/CardGatewayController`.

Gateway route groups include:

- `POST /card/create`
- `PUT /card/update`
- `GET /card/health`

## Contracts

gRPC types come from `com.burov:contracts` version `0.0.24`. Account and card event payloads come
from `com.burov:kafka-contracts`. Shared outbox and processed-event helpers come from
`com.burov:support` version `0.0.1`.
