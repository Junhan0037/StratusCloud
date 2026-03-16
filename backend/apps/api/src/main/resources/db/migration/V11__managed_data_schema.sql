-- Week 12 확장 단계의 Managed DB 제어면 스키마를 추가한다.

create table if not exists managed_databases (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    project_id uuid not null references projects(id),
    name varchar(100) not null,
    engine varchar(30) not null,
    engine_version varchar(30) not null,
    instance_class varchar(40) not null,
    storage_gb integer not null,
    status varchar(30) not null,
    deleted_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create unique index if not exists uk_managed_databases_project_name
    on managed_databases(project_id, name);

create index if not exists idx_managed_databases_project_created_at
    on managed_databases(project_id, created_at desc);

create table if not exists managed_database_backups (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    project_id uuid not null references projects(id),
    database_id uuid not null references managed_databases(id),
    status varchar(30) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create index if not exists idx_managed_database_backups_database_created_at
    on managed_database_backups(database_id, created_at desc);

create table if not exists managed_database_metrics (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    project_id uuid not null references projects(id),
    database_id uuid not null references managed_databases(id),
    cpu_percent integer not null,
    memory_percent integer not null,
    storage_used_gb integer not null,
    connection_count integer not null,
    recorded_at timestamp not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create index if not exists idx_managed_database_metrics_database_recorded_at
    on managed_database_metrics(database_id, recorded_at desc);
