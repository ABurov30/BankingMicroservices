--liquibase formatted sql
--changeset andrey:003-drop-outbox-event-key-unique-constraint

alter table push_notification_outbox_events
    drop constraint push_notification_outbox_events_event_key_key;
