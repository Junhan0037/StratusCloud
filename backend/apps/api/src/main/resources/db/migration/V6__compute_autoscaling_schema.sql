-- Week 6에서 사용하는 Compute Autoscaling/Healthcheck 스키마를 추가한다.

create table if not exists compute_autoscaling_groups (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    project_id uuid not null references projects(id),
    name varchar(100) not null,
    image_id uuid not null references compute_images(id),
    flavor varchar(20) not null,
    min_instances integer not null,
    max_instances integer not null,
    desired_instances integer not null,
    cpu_scale_out_percent integer not null,
    cpu_scale_in_percent integer not null,
    memory_scale_out_percent integer not null,
    memory_scale_in_percent integer not null,
    health_policy varchar(20) not null,
    failure_threshold integer not null,
    status varchar(20) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create index if not exists idx_compute_autoscaling_groups_project_created_at
    on compute_autoscaling_groups(project_id, created_at desc);

alter table compute_instances
    add column if not exists autoscaling_group_id uuid references compute_autoscaling_groups(id);

alter table compute_instances
    add column if not exists health_status varchar(20) not null default 'UNKNOWN';

alter table compute_instances
    add column if not exists restart_count integer not null default 0;

create index if not exists idx_compute_instances_autoscaling_group
    on compute_instances(autoscaling_group_id, created_at desc);

create table if not exists compute_instance_metrics (
    id uuid primary key,
    instance_id uuid not null unique references compute_instances(id),
    cpu_percent integer not null,
    memory_percent integer not null,
    reported_at timestamp not null,
    reported_by varchar(100) not null
);

create table if not exists compute_instance_health_checks (
    id uuid primary key,
    instance_id uuid not null unique references compute_instances(id),
    status varchar(20) not null,
    failure_count integer not null,
    detail text,
    checked_at timestamp not null
);
