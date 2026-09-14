--liquibase formatted sql
--changeset andrey:008-outbox-processing

alter table card_outbox_events drop constraint chk_card_outbox_event_status;
alter table card_outbox_events add constraint chk_card_outbox_event_status
    check (status in ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED'));

create index idx_card_outbox_pending on card_outbox_events (created_at, id)
    include (next_retry_at, retry_count) where status = 'PENDING';
create index idx_card_outbox_lease on card_outbox_events (locked_at, created_at, id)
    where status = 'PROCESSING';
