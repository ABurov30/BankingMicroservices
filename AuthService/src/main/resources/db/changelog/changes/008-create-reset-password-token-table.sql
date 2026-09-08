--liquibase formatted sql
--changeset andrey:008-create-reset-password-token-table

create table reset_password_tokens
(
    id         uuid primary key,
    user_id    uuid         not null,
    token_hash varchar(255) not null unique,
    expires_at timestamp    not null,
    used_at    timestamp,
    created_at timestamp    not null,

    constraint fk_refresh_token_auth_user
        foreign key (user_id)
            references auth_users (id)
);