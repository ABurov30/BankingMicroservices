--liquibase formatted sql
--changeset andrey:003-drop-outbox-event-key-unique-constraint

alter table transaction_outbox_events
    drop constraint transaction_outbox_events_event_key_key;
