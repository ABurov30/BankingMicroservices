--liquibase formatted sql
--changeset andrey:008-use-minor-units-for-account-balances

alter table accounts
    add column available_balance_minor_units bigint,
    add column reserved_balance_minor_units bigint;

update accounts a
set available_balance_minor_units =
        round(a.available_balance * case c.name
            when 'USD' then 100
            when 'EUR' then 100
            when 'GBP' then 100
            when 'CNY' then 100
            else 100
        end)::bigint,
    reserved_balance_minor_units =
        round(a.reserved_balance * case c.name
            when 'USD' then 100
            when 'EUR' then 100
            when 'GBP' then 100
            when 'CNY' then 100
            else 100
        end)::bigint
from currencies c
where a.currency_id = c.id;

alter table accounts
    alter column available_balance_minor_units set default 0,
    alter column reserved_balance_minor_units set default 0,
    alter column available_balance_minor_units set not null,
    alter column reserved_balance_minor_units set not null;

alter table accounts
    drop constraint chk_accounts_available_balance_non_negative,
    drop constraint chk_accounts_reserved_balance_non_negative;

alter table accounts
    drop column available_balance,
    drop column reserved_balance;

alter table accounts
    add constraint chk_accounts_available_balance_minor_units_non_negative
        check (available_balance_minor_units >= 0),
    add constraint chk_accounts_reserved_balance_minor_units_non_negative
        check (reserved_balance_minor_units >= 0);

alter table account_holds
    alter column minor_units type bigint using round(minor_units)::bigint;

alter table account_holds
    drop constraint chk_accounts_minor_units_positive;

alter table account_holds
    add constraint chk_account_holds_minor_units_positive
        check (minor_units > 0);
