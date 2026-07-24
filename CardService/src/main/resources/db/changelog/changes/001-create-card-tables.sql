--liquibase formatted sql
--changeset andrey:001-create-user-tables

create table cards
(
    id            uuid primary key,
    account_id    uuid        not null,
    pan           varchar(19) not null unique,
    status        varchar(55) not null,
    daily_limit   decimal,
    monthly_limit decimal,
    expires_at    timestamp   not null,
    created_at    timestamp   not null,

    constraint chk_cards_status
        check ( status in ('ACTIVE', 'BLOCKED', 'EXPIRED', 'FROZEN')),

    constraint chk_card_pan_format
        check (pan ~ '^[0-9]{12,19}$')
);

create table account_ownership_projection
(
    account_id         uuid PRIMARY KEY,
    owner_auth_user_id uuid not null
);

create index idx_cards_account_id on cards(account_id);
