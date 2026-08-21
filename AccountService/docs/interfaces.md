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

`topUpAccount` and `withdrawAccount` receive `minorUnits` and convert them to the account currency amount before updating balances.

`reserveFundsForTransaction` receives `minorUnits` from `TransactionService` and converts them to the source account currency amount before comparing and reserving balances.

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
