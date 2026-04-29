alter table events
    add column start_time time not null default '00:00:00' after end_date,
    add column end_time time not null default '23:59:00' after start_time;

alter table events
    add constraint chk_events_time_range
        check (start_date < end_date or start_time <= end_time);
