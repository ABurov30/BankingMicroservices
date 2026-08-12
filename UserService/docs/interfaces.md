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
- `POST /user/user-info`
- `GET /user/manager/all-user-info`
- `GET /user/manager/user-info/{userId}`
- `GET /user/health`

## Contracts

gRPC types come from `com.burov:contracts`. Auth event payloads come from `com.burov:kafka-contracts`.
