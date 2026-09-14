--liquibase formatted sql
--changeset andrey:004-outbox-processing

alter table push_notification_outbox_events drop constraint chk_auth_outbox_status;
alter table push_notification_outbox_events add constraint chk_auth_outbox_status
    check (status in ('PENDING', 'PROCESSING', 'PUBLISHED', 'FAILED'));

create index idx_push_notification_outbox_pending on push_notification_outbox_events (created_at, id)
    include (next_retry_at, retry_count) where status = 'PENDING';
create index idx_push_notification_outbox_lease on push_notification_outbox_events (locked_at, created_at, id)
    where status = 'PROCESSING';
