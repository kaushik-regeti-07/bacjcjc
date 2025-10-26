create table if not exists path_configs (
    id serial primary key,
    prefix varchar(100) not null unique,
    source_path varchar(512) not null,
    output_path varchar(512) not null,
    status varchar(16) not null,
    created_at timestamp with time zone default now() not null
);

create table if not exists routing_logs (
    id serial primary key,
    file_name varchar(512) not null,
    action varchar(16) not null,
    from_path varchar(512) not null,
    to_path varchar(512) not null,
    message varchar(1000),
    created_at timestamp with time zone default now() not null
);
