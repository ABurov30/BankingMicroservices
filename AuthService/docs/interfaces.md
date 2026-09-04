# AuthService Interfaces

[Docs Index](README.md)

## gRPC Service

Service implementation: `AuthGrpcService`.

| Operation | Purpose |
| --- | --- |
| `getAuthHealth` | Health check |
| `signup` | Create a new auth user and emit signup-related events |
| `login` | Validate credentials and return access/refresh tokens |
| `logout` | Revoke refresh token |
| `refresh` | Exchange refresh token for a new token pair |
| `changePassword` | Change password for an authenticated user |
| `blockAuthUser` | Manager/admin user blocking |
| `unlockAuthUser` | Manager/admin user unlocking |
| `verifyAuthUserByPrivilegeRole` | Manager/admin verification |
| `verifyAuthUserByCode` | Code-based user verification |
| `changeAuthUserRole` | Admin role assignment flow |
| `getAuthUserById` | Lookup auth user data, including linked social provider accounts |
| `forgetPassword` | Start password reset flow |
| `resetPassword` | Complete password reset flow |

## REST Exposure

Auth REST endpoints are exposed through `ApiGateway/AuthGatewayController`, not directly through this service's controllers.

## Contracts

gRPC request and response types come from `com.burov:contracts` version `0.0.25`. Kafka event
types come from `com.burov:kafka-contracts`. Shared outbox helpers come from `com.burov:support`
version `0.0.1`.

`GetAuthUserByIdGrpcResponse` returns the auth user id, status, email, role, and a repeated
`SocialAccountResponse` list with each linked provider and provider email.
