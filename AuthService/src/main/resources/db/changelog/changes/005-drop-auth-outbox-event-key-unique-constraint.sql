--liquibase formatted sql
--changeset andrey:005-drop-auth-outbox-event-key-unique-constraint

alter table auth_outbox_events
    drop constraint auth_outbox_events_event_key_key;
