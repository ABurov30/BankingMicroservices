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

- Card responses map `CardResponse` from `CardService`, including configured limits and spend counters: `dailyLimit`, `monthlyLimit`, `spendDailyLimit`, and `spendMonthlyLimit`.
- Transaction creation requests require `sourceCardId`; the transaction flow uses it for card limit reservation before account funds are requested.
- User auth info responses include linked social provider accounts as `socialAccounts`.
- `POST /user/user-info` returns user accounts with cards but omits account `availableBalance` and `reservedBalance`.

## WebSocket

The gateway maps authenticated users to WebSocket principals and sends push notifications to `/queue/notifications`.
