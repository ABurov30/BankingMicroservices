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

`createAccount` returns `ALREADY_EXISTS` when the owner already has an account with the requested currency and account type.

`topUpAccount` and `withdrawAccount` receive `minorUnits` and apply them directly to account
balances stored in minor units.

`reserveFundsForTransaction` receives `minorUnits` from `TransactionService` and compares/reserves
them directly against the source account minor-unit balances.

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

gRPC types come from `com.burov:contracts` version `0.0.23`. Event payloads come from
`com.burov:kafka-contracts`. Shared outbox, processed-event, and money-unit helpers come from
`com.burov:support` version `0.0.1`.
