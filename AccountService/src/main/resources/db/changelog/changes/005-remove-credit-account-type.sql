--liquibase formatted sql
--changeset andrey:005-remove-credit-account-type

update accounts
set type = 'CHECKING'
where type = 'CREDIT';

alter table accounts
    drop constraint chk_accounts_type;

alter table accounts
    add constraint chk_accounts_type
        check (type in ('CHECKING', 'SAVINGS'));
