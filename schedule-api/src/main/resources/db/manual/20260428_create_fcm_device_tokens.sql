create table fcm_device_tokens (
    token varchar(512) not null,
    user_id varchar(40) not null,
    group_id varchar(40) not null,
    platform varchar(30) null,
    created_at datetime(3) not null,
    updated_at datetime(3) not null,
    primary key (token)
);

create index idx_fcm_device_tokens_user_id
    on fcm_device_tokens (user_id);

create index idx_fcm_device_tokens_group_id
    on fcm_device_tokens (group_id);
