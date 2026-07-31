--liquibase formatted sql
--changeset andrey:004-drop-user-outbox-event-key-unique-constraint

alter table user_outbox_events
    drop constraint user_outbox_events_event_key_key;
