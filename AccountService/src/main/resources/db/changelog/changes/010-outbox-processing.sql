--liquibase formatted sql
--changeset andrey:010-outbox-processing

alter table account_outbox_events drop constraint chk_account_outbox_status;
alter table account_outbox_events add constraint chk_account_outbox_status
    check (status in ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED'));

create index idx_account_outbox_pending on account_outbox_events (created_at, id)
    include (next_retry_at, retry_count) where status = 'PENDING';
create index idx_account_outbox_lease on account_outbox_events (locked_at, created_at, id)
    where status = 'PROCESSING';
