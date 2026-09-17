--liquibase formatted sql

--changeset aburov:011-allow-cache-invalidation-outbox-event
ALTER TABLE account_outbox_events
    DROP CONSTRAINT IF EXISTS chk_account_outbox_event_type;

ALTER TABLE account_outbox_events
    ADD CONSTRAINT chk_account_outbox_event_type
        CHECK (event_type IN (
            'ACCOUNT_CREATED',
            'ACCOUNT_FROZEN',
            'ACCOUNT_UNFROZEN',
            'TRANSACTION_COMPENSATED',
            'TRANSACTION_COMPLETED',
            'CACHE_INVALIDATION'
        ));
