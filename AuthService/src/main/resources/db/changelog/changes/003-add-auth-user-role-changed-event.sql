--liquibase formatted sql
--changeset andrey:003-add-auth-user-role-changed-event

alter table auth_outbox_events
    drop constraint if exists chk_auth_outbox_event_type;

alter table auth_outbox_events
    add constraint chk_auth_outbox_event_type
        check (event_type in ('AUTH_USER_CREATED', 'AUTH_USER_BLOCKED', 'AUTH_USER_UNLOCK', 'AUTH_USER_VERIFIED', 'AUTH_USER_ROLE_CHANGED'));
