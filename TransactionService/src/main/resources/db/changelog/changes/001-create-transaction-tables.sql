--liquibase formatted sql
--changeset andrey:001-create-transaction-tables

create table transactions
(
    id                uuid primary key,
    source_account_id uuid        not null,
    target_account_id uuid        not null,
    idempotency_key   uuid        not null unique,
    amount            decimal     not null,
    currency          varchar(55) not null,
    status            varchar(55) not null default 'FUNDS_RESERVED',
    error_message     varchar(255),
    created_at        timestamp   not null,
    completed_at      timestamp,
    updated_at        timestamp   not null,

    constraint chk_transaction_status
        check ( status in ('FUNDS_RESERVED', 'FUNDS_REQUESTED', 'COMPLETED', 'FAILED', 'COMPENSATED') ),

    constraint chk_transactions_different_accounts
        check (source_account_id <> target_account_id),

    constraint chk_transactions_currency
        check ( currency in ('USD', 'EUR', 'CNY', 'GBP') ),

    constraint chk_transaction_amount_positive
        check ( amount > 0 )
);

create table transaction_outbox_events
(
    id             uuid primary key,
    aggregate_type varchar(255) not null,
    aggregate_id   uuid         not null,
    event_type     varchar(100) not null,
    payload        jsonb        not null,
    status         varchar(50)  not null default 'PENDING',
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

    constraint chk_transaction_outbox_event_type
        check (event_type in ('TRANSACTION_FUNDS_REQUESTED', 'TRANSACTION_COMPLETED', 'TRANSACTION_FAILED')),

    constraint chk_transaction_outbox_status
        check (status in ('PENDING', 'PUBLISHED', 'FAILED'))
);

create table processed_events
(
    id           uuid primary key,
    event_key    varchar(255) not null unique,
    processed_at timestamp    not null default current_timestamp
);