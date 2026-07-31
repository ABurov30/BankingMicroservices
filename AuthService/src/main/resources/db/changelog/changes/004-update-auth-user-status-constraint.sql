--liquibase formatted sql
--changeset andrey:004-update-auth-user-status-constraint

alter table auth_users
    drop constraint if exists chk_auth_users_status;

alter table auth_users
    add constraint chk_auth_users_status
        check ( status in ('ACTIVE', 'BLOCKED', 'PENDING', 'FORGET_PASSWORD') );