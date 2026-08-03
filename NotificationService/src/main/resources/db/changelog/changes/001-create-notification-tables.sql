--liquibase formatted sql
--changeset andrey:001-create-user-tables

create table push_notification_outbox_events
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
        check (event_type in ('PUSH_NOTIFICATION_CREATED')),

    constraint chk_auth_outbox_status
        check (status in ('PENDING', 'PUBLISHED', 'FAILED'))
);
