--liquibase formatted sql
--changeset andrey:002-create-processed-event-table

create table processed_events
(
    id           uuid primary key,
    event_key    varchar(255) not null unique,
    processed_at timestamp    not null default current_timestamp
);
