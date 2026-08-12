# AuthService Events

[Docs Index](README.md)

## Produced Events

Auth lifecycle changes are written to `AuthOutboxEventEntity` and published by `AuthOutboxPublisher`.

Known auth event categories include:

- auth user created
- auth user blocked
- auth user unlocked
- auth user verified
- auth user role changed
- auth user password reset or forget-password flow events

## Consumed Events

The service has Kafka consumer configuration with group id `auth-service`. Current business logic primarily uses auth-owned state and produced events.

## Consumers

Downstream consumers include:

- `UserService` for user profile projection changes.
- `NotificationService` for email and push notifications.
- `ApiGateway` indirectly through JWT claims and auth gRPC responses.

## Agent Notes

When changing event payloads or topics, update the contracts package first and then update every consumer.
