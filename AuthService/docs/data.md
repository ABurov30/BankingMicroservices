# AuthService Data and Persistence

[Docs Index](README.md)

## Storage

Primary storage is PostgreSQL. Schema changes are managed by Liquibase under `src/main/resources/db/changelog`.

## Entities

| Entity | Purpose |
| --- | --- |
| `AuthUserEntity` | Auth user credentials, verification state, status, monotonic access-state version, and optimistic-lock version |
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

Refresh token and user status changes affect security. `access_state_version` advances on every
block/unlock transition and is included in the `AUTH_USER_STATUS_CHANGED` outbox payload. Avoid
bypassing service methods that enforce status checks and event publication.
The separate JPA `version` column prevents concurrent writes from silently overwriting an auth
user state transition.
Changing a password revokes all active refresh tokens for the auth user and stores one replacement
refresh token for the current session.

Auth outbox rows use shared outbox helpers from `com.burov:support`.

Social account rows are unique by provider and provider user id. A single auth user may have
multiple linked social provider accounts.

## Outbox attempts

The outbox status constraint now permits PROCESSING. The `*-outbox-processing.sql` migration
adds partial pending and lease indexes. `locked_by` is a unique attempt token;
`retry_count` increments on claim. Claims and callbacks use separate transactions.
See [shared outbox publishing](../../docs/outbox.md) for settings, guarantees, metrics and rollout.

The optional startup administrator seed writes `auth_users` and `user_roles` atomically,
using the existing ADMIN role as a database lock. It does not alter existing users or issue tokens.
See [first administrator](configuration.md#first-administrator).
