--liquibase formatted sql
--changeset andrey:003-create-card-limit-hold-table

create table card_limit_holds
(
    id             uuid primary key,
    card_id        uuid        not null,
    transaction_id uuid        not null,
    amount         decimal     not null,
    status         varchar(55) not null,
    expires_at     timestamp,
    created_at     timestamp   not null,
    released_at    timestamp,

    constraint chk_card_limit_holds_status
        check ( status in ('RESERVED', 'RELEASED', 'RELEASED_BY_TIME', 'COMPENSATED', 'FAILED') ),

    constraint chk_accounts_amount_positive
        check ( amount > 0 ),

    constraint uq_card_limit_holds_transaction_id
        unique ( transaction_id )
);

alter table cards
    add column spend_daily_limit decimal not null default 0;

alter table cards
    add constraint chk_cards_spend_daily_limit_positive
        check ( spend_daily_limit >= 0 );

alter table cards
    add column spend_monthly_limit decimal not null default 0;

alter table cards
    add constraint chk_cards_spend_monthly_limit_positive
        check ( spend_monthly_limit >= 0 );
