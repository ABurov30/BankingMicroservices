--liquibase formatted sql
--changeset andrey:003-add-account-unfrozen-outbox-event-type

alter table account_outbox_events
    drop constraint chk_auth_outbox_event_type;

alter table account_outbox_events
    add constraint chk_auth_outbox_event_type
        check (event_type in ('ACCOUNT_CREATED', 'ACCOUNT_FROZEN', 'ACCOUNT_UNFROZEN'));
