--liquibase formatted sql
--changeset andrey:001-create-user-tables

create table user_profiles
(
    id           uuid primary key,
    auth_user_id uuid unique  not null,
    email        varchar(255) not null,
    first_name   varchar(55)  not null,
    last_name    varchar(55)  not null,
    status       varchar(55)  not null default 'PENDING',
    created_at   timestamp    not null,
    updated_at   timestamp    not null,

    constraint chk_user_profiles_status
        check ( status in ('ACTIVE', 'BLOCKED', 'PENDING') )
);

create table user_outbox_events
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

    constraint chk_auth_outbox_event_type
        check (event_type in ('USER_PROFILE_CREATED', 'USER_PROFILE_BLOCKED', 'USER_PROFILE_UNLOCK')),

    constraint chk_auth_outbox_status
        check (status in ('PENDING', 'PUBLISHED', 'FAILED'))
);
