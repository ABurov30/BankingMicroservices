--liquibase formatted sql
--changeset andrey:001-create-auth-tables

create extension if not exists pgcrypto;

create table auth_users
(
    id             uuid primary key,
    password_hash  varchar(255) not null,
    status         varchar(50)  not null,
    email          varchar(255) not null unique,
    email_verified boolean      not null default false,
    created_at     timestamp    not null,
    updated_at     timestamp    not null,

        constraint chk_auth_users_status
            check ( status in ('ACTIVE', 'BLOCKED', 'PENDING') )
);

create table refresh_tokens
(
    id         uuid primary key,
    user_id    uuid         not null,
    token_hash varchar(255) not null unique,
    expires_at timestamp    not null,
    revoked_at timestamp,
    created_at timestamp    not null,

    constraint fk_refresh_token_auth_user
        foreign key (user_id)
            references auth_users (id)
);

create table roles
(
    id   uuid primary key,
    name varchar(255) not null unique,

        constraint chk_roles_name
            check (name in ('USER', 'MANAGER', 'ADMIN'))
);

insert into roles (id, name)
values (gen_random_uuid(), 'USER'),
       (gen_random_uuid(), 'MANAGER'),
       (gen_random_uuid(), 'ADMIN');

create table user_roles
(
    id      uuid primary key,
    user_id uuid not null,
    role_id uuid not null,

    constraint fk_user_role_auth_user
        foreign key (user_id)
            references auth_users (id),

    constraint fk_user_role_role
        foreign key (role_id)
            references roles (id),

    constraint uq_user_role_user_id unique (user_id)
);

create table auth_outbox_events
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
        check (event_type in ('AUTH_USER_CREATED')),

    constraint chk_auth_outbox_status
        check (status in ('PENDING', 'PUBLISHED', 'FAILED'))
);
