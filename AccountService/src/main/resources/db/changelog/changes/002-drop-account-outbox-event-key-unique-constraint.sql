--liquibase formatted sql
--changeset andrey:002-drop-account-outbox-event-key-unique-constraint

alter table account_outbox_events
    drop constraint account_outbox_events_event_key_key;
