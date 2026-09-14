--liquibase formatted sql
--changeset andrey:004-outbox-processing

alter table transaction_outbox_events drop constraint chk_transaction_outbox_status;
alter table transaction_outbox_events add constraint chk_transaction_outbox_status
    check (status in ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED'));

create index idx_transaction_outbox_pending on transaction_outbox_events (created_at, id)
    include (next_retry_at, retry_count) where status = 'PENDING';
create index idx_transaction_outbox_lease on transaction_outbox_events (locked_at, created_at, id)
    where status = 'PROCESSING';
