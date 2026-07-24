--liquibase formatted sql
--changeset andrey:002-update-user-outbox-event-types

alter table user_outbox_events
    drop constraint if exists chk_auth_outbox_event_type;

alter table user_outbox_events
    add constraint chk_auth_outbox_event_type
        check (event_type in ('USER_PROFILE_CREATED', 'USER_PROFILE_BLOCKED', 'USER_PROFILE_UNLOCK'));
