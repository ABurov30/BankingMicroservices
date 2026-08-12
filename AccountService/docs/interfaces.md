# AccountService Interfaces

[Docs Index](README.md)

## gRPC Service

Service implementation: `AccountGrpcService`.

| Operation | Purpose |
| --- | --- |
| `getAccountHealth` | Health check |
| `createAccount` | Create a new account |
| `getAccountsByOwnerUserId` | Read accounts owned by a user |
| `getAllAccounts` | Read all accounts for manager/admin flows |
| `freezeAccount` | Freeze an account |
| `unfreezeAccount` | Unfreeze an account |
| `getAccountById` | Read a single account |
| `topUpAccount` | Add funds to an account |
| `withdrawAccount` | Withdraw funds from an account |
| `reserveFundsForTransaction` | Reserve funds for transaction processing |

## REST Exposure

Account REST endpoints are exposed through `ApiGateway/AccountGatewayController`.

Gateway route groups include:

- `POST /account/create`
- `GET /account/accounts/{ownerUserId}`
- `PUT /account/freeze/{accountId}`
- `PUT /account/unfreeze/{accountId}`
- `GET /account/manager/all-accounts`
- `POST /account/topUp`
- `POST /account/withdraw`
- `GET /account/health`

## Contracts

gRPC types come from `com.burov:contracts`. Event payloads come from `com.burov:kafka-contracts`.
