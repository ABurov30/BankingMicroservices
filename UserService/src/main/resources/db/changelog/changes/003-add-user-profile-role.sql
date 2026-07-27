--liquibase formatted sql
--changeset andrey:003-add-user-profile-role

alter table user_profiles
    add column role varchar(55) not null default 'USER';

alter table user_profiles
    add constraint chk_user_profiles_role
        check (role in ('USER', 'MANAGER', 'ADMIN'));
