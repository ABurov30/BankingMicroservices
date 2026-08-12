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
- `/ws` and `/ws/**`
- `/*/health`
- `/v3/api-docs/**`
- `/swagger-ui/**`
- `/swagger-ui.html`
- `/actuator/health`

All other routes require an active user or an admin role unless a narrower manager/admin rule applies.

## gRPC Clients

| Client | Target service |
| --- | --- |
| `AuthGrpcClient` | `AuthService` |
| `UserGrpcClient` | `UserService` |
| `AccountGrpcClient` | `AccountService` |
| `CardGrpcClient` | `CardService` |
| `TransactionGrpcClient` | `TransactionService` |
| `NotificationGrpcClient` | `NotificationService` |

## WebSocket

The gateway maps authenticated users to WebSocket principals and sends push notifications to `/queue/notifications`.
