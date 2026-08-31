--liquibase formatted sql
--changeset andrey:002-use-minor-units-for-transactions

alter table transactions
    alter column minor_units type bigint using round(minor_units)::bigint;