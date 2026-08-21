--liquibase formatted sql
--changeset andrey:004-store-card-limits-in-minor-units

alter table cards
    rename column daily_limit to daily_limit_minor_units;

alter table cards
    rename column monthly_limit to monthly_limit_minor_units;

alter table cards
    rename column spend_daily_limit to spend_daily_limit_minor_units;

alter table cards
    rename column spend_monthly_limit to spend_monthly_limit_minor_units;

alter table card_limit_holds
    rename column amount to minor_units;

alter table card_limit_holds
    rename constraint chk_accounts_amount_positive to chk_card_limit_holds_minor_units_positive;
