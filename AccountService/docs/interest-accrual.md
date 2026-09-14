# Savings interest accrual

[Docs Index](README.md)

## Business date and retry

`AccountScheduler.updateAccountsBalances()` runs at midnight in `Europe/Paris`.
It derives one `LocalDate` using that zone at the start of the run and passes that
same date to every account operation, even if processing crosses midnight. The
injected `Clock` makes date selection testable and independent of the host timezone.

The scheduler selects SAVINGS account IDs. An empty selection succeeds. It invokes
`AccountInterestService.accrueInterest(accountId, businessDate)` through a separate
Spring bean, with a new transaction for each account. Failed accounts are logged
and the batch continues. A retry skips committed accruals and retries failed ones.
For an explicit retry, call `updateAccountsBalances(originalBusinessDate)` on the
scheduler bean; there is no new HTTP or gRPC endpoint.

A restart does not automatically replay missed dates. The no-argument entrypoint
always uses today's Paris date. An explicit retry must retain the original date.
Historical backfills use the current balance, not a reconstructed historical balance;
this change does not introduce historical balance accounting or automatic catch-up.

## Atomicity and identity

`account_interest_accruals` stores a UUID `id`, `account_id`, `accrual_date`, the
rounded interest delta in `amount_minor_units`, the annual percentage `rate`
(currently `7`), and a database-generated `created_at` timestamp with timezone.
The unique `(account_id, accrual_date)` constraint represents the logical key
`interest:{accountId}:{accrualDate}`. The logical key is not stored a second time.
The account foreign key prevents orphaned history; no cascading deletion is configured.

Inside a single transaction, the operation:

1. Locks the account using `PESSIMISTIC_WRITE` and checks SAVINGS eligibility and history.
2. Calculates the new balance and inserts the accrual with `ON CONFLICT DO NOTHING`.
3. Updates and flushes the balance only if the insert succeeded.

A duplicate returns `false` without changing the balance. PostgreSQL handles the
unique conflict without poisoning the transaction. The history insert and balance
update commit or roll back together. Locks are held for one account at a time;
independent service replicas coordinate through the same PostgreSQL constraints and locks.
Even zero rounded interest creates history, so a later balance change cannot cause
another accrual for that date. Do not remove history to retry a failed run.

## Calculation

The existing calculation is preserved:

```text
dailyMultiplier = 1 + roundHalfUp(7 / 36500, 12)
newBalance = toMinor(toMajor(availableBalanceMinorUnits, currency) * dailyMultiplier, currency)
amountMinorUnits = newBalance - previousBalance
```

`MoneyUnitsConverter` uses the currency's minor-unit scale and HALF_EVEN rounding.
Only available balance earns interest; reserved balance is unchanged. The denominator
remains 365 days in leap years as in the previous implementation. Overflow fails and
rolls back the account transaction. Account status filtering is unchanged: SAVINGS
accounts are selected irrespective of active/frozen status.

## Observability

`account.interest.accruals` is a counter tagged with `result=processed|skipped|failed`:

- `processed`: the account transaction committed, including zero interest.
- `skipped`: that date was already processed or the account is no longer SAVINGS.
- `failed`: the account operation or transaction commit failed.

Results are counted after the transactional service returns. No account IDs or dates
are metric tags. Logs include the business date and final processed/skipped/failed
counts; failure logs also include account ID and logical idempotency key. Failures
while obtaining the initial account list abort the run before per-account metrics.

## Deployment and verification

Apply migration `009-create-account-interest-accruals.sql` before enabling the new
scheduler. Stop old scheduler replicas first: old code does not consult history.
If the old scheduler already accrued interest today, start the new scheduler on the
next Paris business date, or backfill today's history from verified accounting data
before running it. An empty history table cannot identify prior legacy accruals.

Unit tests cover currency rounding, Paris date selection, duplicates, empty selection,
and result counters. PostgreSQL integration tests cover repeated and concurrent
scheduler instances, the next date, zero interest, unique conflicts, and a database
failure after history insertion followed by retry. Testcontainers starts PostgreSQL;
Kafka and Schema Registry are not needed for these focused tests:

```bash
./mvnw -Pintegration-tests -Dtest=AccountInterestAccrualIT test
```
