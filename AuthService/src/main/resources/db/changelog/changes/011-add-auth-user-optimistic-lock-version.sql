--liquibase formatted sql

--changeset aburov:011-add-auth-user-optimistic-lock-version
ALTER TABLE auth_users
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
