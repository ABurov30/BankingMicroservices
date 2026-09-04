--liquibase formatted sql
--changeset andrey:001-create-user-tables

create table cards
(
    id            uuid primary key,
    account_id    uuid        not null,
    pan           varchar(19) not null unique,
    status        varchar(55) not null,
    currency      varchar(3)  not null,
    daily_limit   decimal,
    monthly_limit decimal,
    expires_at    timestamp   not null,
    created_at    timestamp   not null,

    constraint chk_cards_status
        check ( status in ('ACTIVE', 'BLOCKED', 'EXPIRED', 'FROZEN')),

    constraint chk_card_pan_format
        check (pan ~ '^[0-9]{12,19}$'),

    constraint chk_cards_currency
        check (currency in ('USD', 'EUR', 'CNY', 'GBP'))
);

create table account_ownership_projection
(
    account_id         uuid PRIMARY KEY,
    owner_auth_user_id uuid not null,
    account_number     varchar(55)
);

create index idx_cards_account_id on cards (account_id);

--changeset andrey:002-add-card-outbox-events

alter table account_ownership_projection
    add column if not exists account_number varchar(55);

create table if not exists card_outbox_events
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

    constraint chk_card_outbox_event_status
        check (status in ('PENDING', 'PUBLISHED', 'FAILED') ),

    constraint chk_card_outbox_event_type
        check ( event_type in ('CARD_CREATED', 'CARD_FROZEN', 'CARD_UNFROZEN') )
);
