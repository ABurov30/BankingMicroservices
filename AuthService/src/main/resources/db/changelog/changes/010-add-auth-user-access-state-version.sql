--liquibase formatted sql

--changeset aburov:010-add-auth-user-access-state-version
ALTER TABLE auth_users
    ADD COLUMN access_state_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE auth_outbox_events
    DROP CONSTRAINT IF EXISTS chk_auth_outbox_event_type;

ALTER TABLE auth_outbox_events
    ADD CONSTRAINT chk_auth_outbox_event_type
        CHECK (event_type IN (
            'AUTH_USER_CREATED',
            'AUTH_USER_STATUS_CHANGED',
            'AUTH_USER_VERIFIED',
            'AUTH_USER_ROLE_CHANGED',
            'AUTH_USER_FORGET_PASSWORD',
            'AUTH_SOCIAL_ACCOUNT_AUTH_USER_CREATED'
        ));
