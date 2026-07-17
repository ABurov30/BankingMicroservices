--liquibase formatted sql
--changeset andrey:001-create-user-tables

create table accounts
(
    id                uuid primary key,
    owner_user_id     uuid        not null,
    account_number    varchar(55) not null unique,
    type              varchar(55) not null default 'CHECKING',
    status            varchar(55) not null default 'ACTIVE',
    available_balance decimal     not null default 0,
    reserved_balance  decimal     not null default 0,
    version           integer     not null default 0,
    currency          varchar(55) not null default 'RUB',
    created_at        timestamp   not null,
    updated_at        timestamp   not null,

    constraint chk_accounts_type
        check ( type in ('CHECKING', 'SAVINGS', 'CREDIT') ),

    constraint chk_accounts_status
        check ( status in ('ACTIVE', 'FROZEN', 'CLOSED') ),

    constraint chk_accounts_currency
        check ( currency in ('RUB', 'USD', 'EUR', 'CNY', 'GBP') )

)

create table account_outbox_events
(
    id             uuid primary key,
    aggregate_type varchar(255) not null,
    aggregate_id   uuid         not null,
    event_type     varchar(100) not null,
    payload        jsonb        not null,
    status         varchar(50)  not null default 'ACTIVE',
    retry_count    integer      not null default 0,
    error_message  text,
    created_at     timestamp    not null,
    sent_at        timestamp,
    topic          varchar(50)  not null,
    event_key      varchar(255) not null unique,
    schema_version varchar(255) not null,
    next_retry_at  timestamp,
    locked_at      timestamp,
    locked_by      varchar(255),
    correlation_id uuid,

    constraint chk_auth_outbox_event_type
        check (event_type in ('ACCOUNT_CREATED')),

    constraint chk_auth_outbox_status
        check (status in ('PENDING', 'PUBLISHED', 'FAILED'))
);
