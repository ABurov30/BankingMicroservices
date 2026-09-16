--liquibase formatted sql
--changeset andrey:005-add-transaction-saga-statuses

alter table transactions
    drop constraint chk_transaction_status;

alter table transactions
    add constraint chk_transaction_status
        check (status in (
            'CREATED',
            'CARD_LIMIT_RESERVED',
            'FUNDS_RESERVED',
            'FUNDS_REQUESTED',
            'COMPLETED',
            'FAILED',
            'COMPENSATED'
        ));

alter table transaction_outbox_events
    drop constraint chk_transaction_outbox_event_type;

alter table transaction_outbox_events
    add constraint chk_transaction_outbox_event_type
        check (event_type in (
            'TRANSACTION_FUNDS_REQUESTED',
            'TRANSACTION_COMPLETED',
            'TRANSACTION_FAILED',
            'TRANSACTION_CARD_LIMIT_HOLD_COMPENSATION'
        ));
