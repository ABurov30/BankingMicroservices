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
| `getAccountByIdForTransaction` | Read an account for internal TransactionService flows |
| `topUpAccount` | Add funds to an account |
| `withdrawAccount` | Withdraw funds from an account |
| `reserveFundsForTransaction` | Reserve funds for transaction processing |
| `getRecipientAccountsByOwnerUserId` | Read recipient-safe account data by profile id |

`createAccount` returns `ALREADY_EXISTS` when the owner already has an account with the requested currency and account type.

`getAccountsByOwnerUserId` returns `PERMISSION_DENIED` when the authenticated user neither owns
the account nor has the `ADMIN` or `MANAGER` role.

`getRecipientAccountsByOwnerUserId` returns account ids, account number last-four values, types,
statuses, and currencies. It does not expose balances, full account numbers, or auth-user ids.

`getAccountByIdForTransaction` is an internal service-to-service operation used by
`TransactionService` for transaction responses and status streams. It accepts only `accountId`;
end-user requests must use the ownership-protected `getAccountById` operation.

`topUpAccount` and `withdrawAccount` receive `minorUnits` and apply them directly to account
balances stored in minor units.

`reserveFundsForTransaction` receives `minorUnits` and `currency` from `TransactionService`.
The requested currency must match the source account currency. AccountService compares and reserves
the requested minor units directly against the source account minor-unit balances.

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

gRPC types come from `com.burov:contracts` version `0.0.26`. Event payloads come from
`com.burov:kafka-contracts`. Shared outbox, processed-event, and money-unit helpers come from
`com.burov:support` version `0.0.1`.
