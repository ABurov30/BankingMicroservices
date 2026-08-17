alter table auth_users
    alter column password_hash drop not null,
    alter column verification_code_hash drop not null;

create table auth_social_accounts
(
    id               uuid primary key,
    user_id          uuid         not null references auth_users (id),
    provider         varchar(32)  not null,
    provider_user_id varchar(255) not null,
    created_at       timestamp    not null,

    constraint uq_auth_social_provider_subject
        unique (provider, provider_user_id),

    constraint chk_auth_social_accounts_provider
        check ( provider in ('GOOGLE') )
);