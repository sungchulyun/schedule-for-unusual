create index idx_users_group_created on users (group_id, created_at);
create index idx_users_oauth_provider_user on users (oauth_provider, oauth_provider_user_id);

create index idx_refresh_tokens_user_created on refresh_tokens (user_id, created_at);
create index idx_refresh_tokens_expires_revoked on refresh_tokens (expires_at, revoked_at);

create index idx_events_group_date_deleted on events (group_id, start_date, end_date, deleted_at);
create index idx_events_group_owner_deleted on events (group_id, owner_user_id, deleted_at);

create index idx_shift_group_owner_date_deleted on shift_schedules (group_id, owner_user_id, date, deleted_at);
create index idx_shift_group_date_deleted on shift_schedules (group_id, date, deleted_at);
