--liquibase formatted sql
--changeset andrey:004-create-currency-table
create extension if not exists pgcrypto;

create table currencies
(
    id            uuid primary key,
    name          varchar(55) not null unique,
    rate_from_usd decimal     not null

    constraint chk_currency_name
        check (name in ('USD', 'EUR', 'CNY', 'GBP'))
);

insert into currencies (id, name, rate_from_usd)
values
    (gen_random_uuid(), 'USD', 1),
    (gen_random_uuid(), 'EUR', 1),
    (gen_random_uuid(), 'CNY', 1),
    (gen_random_uuid(), 'GBP', 1);

alter table accounts
    drop constraint chk_accounts_currency;

alter table accounts
    drop column currency;

alter table accounts
    add column currency_id uuid not null;

alter table accounts
    add constraint fk_accounts_currency
        foreign key (currency_id)
            references currencies (id);

