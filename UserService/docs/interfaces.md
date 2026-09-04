# UserService Interfaces

[Docs Index](README.md)

## gRPC Service

Service implementation: `UserGrpcService`.

| Operation | Purpose |
| --- | --- |
| `getUserHealth` | Health check |
| `getUserInfo` | Read profile by user id |
| `getAllUserInfo` | Read all profiles for manager/admin flows |
| `getUserInfoByEmail` | Read profile by email |

## REST Exposure

User REST endpoints are exposed through `ApiGateway/UserGatewayController`.

Gateway route groups include:

- `GET /user/user-info`
- `POST /user/recipient-info`
- `GET /user/manager/all-user-info`
- `POST /user/manager/user-info`
- `GET /user/health`

## Contracts

gRPC types come from `com.burov:contracts` version `0.0.23`. Auth event payloads come from
`com.burov:kafka-contracts`. Shared outbox and processed-event helpers come from
`com.burov:support` version `0.0.1`.
