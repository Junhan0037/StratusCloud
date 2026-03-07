-- Week 5에서 사용하는 Compute Image/Instance 스키마를 추가한다.

create table if not exists compute_images (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    name varchar(100) not null,
    version varchar(50) not null,
    os_type varchar(20) not null,
    status varchar(20) not null,
    tags text not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create unique index if not exists uk_compute_images_tenant_name_version
    on compute_images(tenant_id, name, version);

create index if not exists idx_compute_images_tenant_created_at
    on compute_images(tenant_id, created_at desc);

create table if not exists compute_instances (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    project_id uuid not null references projects(id),
    image_id uuid not null references compute_images(id),
    name varchar(100) not null,
    flavor varchar(20) not null,
    status varchar(20) not null,
    user_data text,
    last_transition_at timestamp not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create index if not exists idx_compute_instances_project_created_at
    on compute_instances(project_id, created_at desc);
