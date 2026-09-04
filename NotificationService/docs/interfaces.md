# NotificationService Interfaces

[Docs Index](README.md)

## HTTP API

Controller: `NotificationController`.

| Endpoint | Purpose |
| --- | --- |
| `GET /notification/notifications` | Read push notifications for an auth user |
| `PATCH /notification/notifications/mark-as-readed` | Mark selected push notifications as read |

These endpoints are also proxied through `ApiGateway/NotificationGatewayController`.

## gRPC Service

Service implementation: `NotificationGrpcService`.

| Operation | Purpose |
| --- | --- |
| `getNotificationHealth` | Health check |

## Contracts

Kafka event payloads come from `com.burov:kafka-contracts`. gRPC health contract types come from
`com.burov:contracts` version `0.0.25`. Shared outbox, processed-event, and money-unit helpers come
from `com.burov:support` version `0.0.1`.
