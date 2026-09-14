--liquibase formatted sql
--changeset andrey:007-drop-outbox-event-key-unique-constraint

alter table card_outbox_events
    drop constraint card_outbox_events_event_key_key;
