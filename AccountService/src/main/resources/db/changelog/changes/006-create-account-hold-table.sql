--liquibase formatted sql
--changeset andrey:006-create-account-hold-table.sql

create table account_holds
(
    id             uuid primary key,
    account_id     uuid        not null,
    transaction_id uuid        not null unique,
    currency       varchar(55) not null,
    minor_units    decimal     not null,
    status         varchar(55) not null,
    expires_at     timestamp,
    created_at     timestamp   not null,
    released_at    timestamp,

    constraint chk_account_holds_status
        check ( status in ('RESERVED', 'RELEASED', 'RELEASED_BY_TIME', 'COMPENSATED', 'FAILED') ),

    constraint chk_account_holds_currency
        check ( currency in ('USD', 'EUR', 'CNY', 'GBP') ),

    constraint chk_accounts_minor_units_positive
        check ( minor_units > 0 )
);