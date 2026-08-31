--liquibase formatted sql
--changeset andrey:006-use-minor-units-for-card-limits


alter table cards
    alter column daily_limit_minor_units type bigint using round(daily_limit_minor_units)::bigint,
    alter column monthly_limit_minor_units type bigint using round(monthly_limit_minor_units)::bigint,
    alter column spend_daily_limit_minor_units type bigint using round(spend_daily_limit_minor_units)::bigint,
    alter column spend_monthly_limit_minor_units type bigint using round(spend_monthly_limit_minor_units)::bigint;

alter table card_limit_holds
    alter column minor_units type bigint using round(minor_units)::bigint;