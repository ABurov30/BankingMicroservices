--liquibase formatted sql
--changeset andrey:007-add-constraint-uq-accounts-owner-user-id-currency-type

alter table accounts
    add constraint uq_accounts_owner_user_id_currency_type
        unique (owner_user_id, currency_id, type);