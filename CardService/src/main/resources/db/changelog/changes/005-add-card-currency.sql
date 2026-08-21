--liquibase formatted sql
--changeset andrey:005-add-card-currency

alter table account_ownership_projection
    add column if not exists currency varchar(3);

update account_ownership_projection
set currency = 'USD'
where currency is null;

alter table account_ownership_projection
    alter column currency set not null;

alter table account_ownership_projection
    add constraint chk_account_ownership_projection_currency
        check (currency in ('USD', 'EUR', 'CNY', 'GBP'));
