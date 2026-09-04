# AuthService Data and Persistence

[Docs Index](README.md)

## Storage

Primary storage is PostgreSQL. Schema changes are managed by Liquibase under `src/main/resources/db/changelog`.

## Entities

| Entity | Purpose |
| --- | --- |
| `AuthUserEntity` | Auth user credentials, verification state, and status |
| `RefreshTokenEntity` | Refresh token state and revocation/expiration data |
| `RoleEntity` | Available roles |
| `UserRoleEntity` | Auth user to role relation |
| `AuthSocialAccountsEntity` | External OAuth provider account linked to an auth user, including provider email |
| `AuthOutboxEventEntity` | Outbox rows for Kafka publishing |

## Repositories

- `AuthUserRepository`
- `RefreshTokenRepository`
- `RoleRepository`
- `UserRoleRepository`
- `AuthSocialAccountsRepository`
- `AuthOutboxEventRepository`

## Migration Files

- `001-create-auth-tables.sql`
- `002-update-auth-verification-events.sql`
- `003-add-auth-user-role-changed-event.sql`
- `004-update-auth-user-status-constraint.sql`
- `005-drop-auth-outbox-event-key-unique-constraint.sql`
- `006-create-auth-social-account-table.sql`
- `007-add-auth-social-account-created-event.sql`

## Data Integrity Notes

Refresh token and user status changes affect security. Avoid bypassing service methods that enforce token revocation, status checks, and event publication.
Changing a password revokes all active refresh tokens for the auth user and stores one replacement
refresh token for the current session.

Auth outbox rows use shared outbox helpers from `com.burov:support`.

Social account rows are unique by provider and provider user id. A single auth user may have
multiple linked social provider accounts.
