--liquibase formatted sql

--changeset aburov:009-allow-cache-invalidation-outbox-event
ALTER TABLE card_outbox_events
    DROP CONSTRAINT IF EXISTS chk_card_outbox_event_type;

ALTER TABLE card_outbox_events
    ADD CONSTRAINT chk_card_outbox_event_type
        CHECK (event_type IN (
            'CARD_CREATED',
            'CARD_FROZEN',
            'CARD_UNFROZEN',
            'CACHE_INVALIDATION'
        ));
