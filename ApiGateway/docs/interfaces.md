# ApiGateway Interfaces

[Docs Index](README.md)

## REST Route Groups

| Route group | Controller | Purpose |
| --- | --- | --- |
| `/auth` | `AuthGatewayController` | Authentication, account verification, password flows, manager/admin auth operations |
| `/user` | `UserGatewayController` | User profile reads and manager user profile reads |
| `/account` | `AccountGatewayController` | Account creation, account reads, freeze/unfreeze, top up, withdrawal |
| `/card` | `CardGatewayController` | Card creation and card update |
| `/transaction` | `TransactionGatewayController` | Transaction creation and user transaction reads |
| `/notification` | `NotificationGatewayController` | Push notification reads and mark-as-read flow |

## Public Endpoints

Security configuration permits:

- `POST /auth/signup`
- `POST /auth/login`
- `POST /auth/refresh`
- `DELETE /auth/logout`
- `PUT /auth/verify-user`
- `GET /auth/oauth/google`
- `/oauth2/**`
- `/login/oauth2/**`
- `/ws` and `/ws/**`
- `/*/health`
- `/v3/api-docs/**`
- `/swagger-ui/**`
- `/swagger-ui.html`
- `/asyncapi`
- `/asyncapi-ui.html`
- `/asyncapi.yaml`
- `/actuator/health`
- `/actuator/prometheus`

All other routes require an active user or an admin role unless a narrower manager/admin rule applies.

## Google OAuth2

`GET /auth/oauth/google` starts Google login by redirecting to Spring Security's Google OAuth2
authorization endpoint. After Google redirects back to `/login/oauth2/code/google`, the gateway
exchanges the OIDC user data with `AuthService`, sets `at` and `rt` cookies, and redirects the
browser to `SITE_URL`.

## gRPC Clients

| Client | Target service |
| --- | --- |
| `AuthGrpcClient` | `AuthService` |
| `UserGrpcClient` | `UserService` |
| `AccountGrpcClient` | `AccountService` |
| `CardGrpcClient` | `CardService` |
| `TransactionGrpcClient` | `TransactionService` |
| `NotificationGrpcClient` | `NotificationService` |

## DTO Notes

- Card responses map `CardResponse` from `CardService`, including card `currency`, configured limits, and spend counters as minor-unit values: `dailyLimitMinorUnits`, `monthlyLimitMinorUnits`, `spendDailyLimitMinorUnits`, and `spendMonthlyLimitMinorUnits`.
- Transaction creation requests require `sourceCardId`; the transaction flow uses it for card limit reservation before account funds are requested.
- Transaction list responses include `transactionId`, which the UI uses to open a live status subscription for the selected transaction.
- User auth info responses include linked social provider accounts as `socialAccounts`.
- `POST /user/user-info` returns user accounts with cards but omits account `availableBalance` and `reservedBalance`.

## WebSocket

The gateway maps authenticated users to WebSocket principals and sends messages through user destinations.
The machine-readable contract is [asyncapi.yaml](asyncapi.yaml). At runtime, the gateway exposes
the rendered AsyncAPI page at `/asyncapi` and `/asyncapi-ui.html`, and the source contract at
`/asyncapi.yaml`.

The STOMP endpoint is `/ws`. The WebSocket handshake uses the HTTP-only `at` cookie to resolve the
authenticated user. Clients subscribe to these user destinations:

| Destination | Message |
| --- | --- |
| `/user/queue/notifications` | `NotificationResponseDto`: `title`, `body`, `type` |
| `/user/queue/transactions/{transactionId}` | `TransactionStatusResponseDto`: `minorUnits`, `currency`, `status`, and optional `sourceAccount`/`targetAccount` fields with `accountNumber`, `currency` |

The transaction list provides the `transactionId` required to build the transaction stream
destination. For the transaction stream itself, `transactionId` is currently part of the destination
and is not included in the protobuf payload. Update both this document and `asyncapi.yaml` when the
public DTO changes.
