--liquibase formatted sql
--changeset andrey:009-create-account-interest-accruals

create table account_interest_accruals
(
    id                 uuid primary key,
    account_id         uuid not null references accounts (id),
    accrual_date       date not null,
    amount_minor_units bigint not null check (amount_minor_units >= 0),
    rate               numeric(19, 12) not null check (rate >= 0),
    created_at         timestamp with time zone not null default current_timestamp,

    constraint uq_account_interest_accrual_date unique (account_id, accrual_date)
);
