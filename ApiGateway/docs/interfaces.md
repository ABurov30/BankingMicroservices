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

The callback is a top-level `GET`. OAuth correlation uses the gateway's `SameSite=Lax` session
cookie; the JWT cookies are not issued until the callback succeeds and remain `SameSite=Strict`.
The OAuth `state` check is performed by Spring Security. This endpoint starts authentication only
and must not be repurposed to change account or payment state.

## Auth Flow Notes

`PUT /auth/change-password` requires an authenticated active user. The request body contains only
`oldPassword` and `newPassword`; the gateway reads `authUserId` from the `at` cookie subject before
calling `AuthService`. On success, `AuthService` revokes active refresh tokens, returns a new
refresh token, and the gateway sets a replacement HTTP-only `rt` cookie. The existing access-token
cookie is not replaced by this endpoint.

## gRPC Clients

| Client | Target service |
| --- | --- |
| `AuthGrpcClient` | `AuthService` |
| `UserGrpcClient` | `UserService` |
| `AccountGrpcClient` | `AccountService` |
| `CardGrpcClient` | `CardService` |
| `TransactionGrpcClient` | `TransactionService` |
| `NotificationGrpcClient` | `NotificationService` |

gRPC DTOs come from `com.burov:contracts` version `0.0.29`. Shared support utilities come from
`com.burov:support` version `0.0.7`.

## DTO Notes

- Card responses map `CardResponse` from `CardService`, including card `currency`, configured limits, and spend counters as minor-unit values: `dailyLimitMinorUnits`, `monthlyLimitMinorUnits`, `spendDailyLimitMinorUnits`, and `spendMonthlyLimitMinorUnits`.
- Transaction creation requests require `sourceCardId`; the transaction flow uses it for card limit reservation before account funds are requested.
- Transaction creation responses include `transactionId`, `minorUnits`, `currency`, and `status`.
- `POST /account/create` accepts only account `type` and `currency`; the account owner is derived
  from the authenticated JWT.
- `GET /account/accounts/me` returns only the caller's accounts and cards. Manager account reads
  remain under `/account/manager/**`.
- User account status changes use `/account/freeze/{accountId}` and
  `/account/unfreeze/{accountId}` and require the `USER` role. The separate manager routes are
  `/account/manager/freeze/{accountId}` and `/account/manager/unfreeze/{accountId}`.
- All `/account/manager/**` and `/transaction/manager/**` endpoints require the `MANAGER`
  or `ADMIN` role.
- `GET /transaction/user/me` returns only transactions for the caller's accounts. Manager reads
  remain under `/transaction/manager/user/{userId}`.
- Transaction list source and target accounts expose only recipient-safe fields: account id, masked
  account number, type, status, and currency. The list includes `transactionId`, which the UI uses
  to open a live status subscription for the selected transaction.
- User auth info responses include linked social provider accounts as `socialAccounts`.
- `POST /user/recipient-info` resolves a recipient by email and returns user info without ids plus
  recipient account ids, account number last-four values, types, statuses, and currencies.
- `GET /user/user-info` returns the current user's profile and auth information.

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

## Account Card Aggregation

All-account, owner-profile, and current-user account lists call `GetCardsByAccountIds`
once with distinct account IDs and the caller's identity and role. Cards are grouped by
`accountId`, preserving account order and returning an empty card list for accounts without
cards. Empty account lists skip the card RPC. Public HTTP response schemas are unchanged.

## User Auth Aggregation

`getAllUserInfoWithAuthInfo` loads profiles once and calls `GetAuthUserByIds` once with
distinct auth user IDs. Auth data is matched by ID, preserving profile order, roles,
auth statuses, and social accounts. Empty profile lists skip the auth RPC; an incomplete
auth response fails with NOT_FOUND. Public HTTP response schemas are unchanged.

## Dependency Failures

Outbound gRPC circuits reject calls with UNAVAILABLE while OPEN or when HALF_OPEN probes are
fully occupied. TransactionService preserves downstream gRPC statuses for its callers.
ApiGateway returns HTTP 503 for UNAVAILABLE, DEADLINE_EXCEEDED, INTERNAL and RESOURCE_EXHAUSTED,
using the existing `ApiErrorResponse` shape. Business status mappings remain unchanged.
There is no successful fallback for reads or writes. Unary read retries are bounded by the
existing deadline; writes are not automatically replayed. See [configuration](configuration.md#grpc-resilience).

For WebSocket transaction subscriptions, circuit rejection terminates upstream stream setup;
no synthetic transaction status is sent and streams are not automatically retried or replayed.

Local gRPC Bulkhead saturation returns RESOURCE_EXHAUSTED (`Downstream bulkhead saturated:
<dependency>-grpc`), mapped to HTTP 503 at ApiGateway. This differs from the UNAVAILABLE
returned by an OPEN circuit. Neither local rejection starts or retries a downstream RPC.

Transaction status subscriptions use a separate `transaction-grpc-stream` semaphore (32 by
default). Saturation rejects upstream setup; no synthetic status message or automatic retry is
sent. The stream holds its permit until completion, error or unsubscribe/cancellation.
