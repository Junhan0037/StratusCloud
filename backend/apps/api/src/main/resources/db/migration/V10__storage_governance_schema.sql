-- Week 10 Storage Governance용 정책, 태그, 미터링 스키마를 추가한다.

create table if not exists governance_storage_policies (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    project_id uuid not null references projects(id),
    max_bucket_count integer,
    max_object_count bigint,
    max_total_bytes bigint,
    presign_per_minute integer,
    upload_per_minute integer,
    download_per_minute integer,
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create unique index if not exists uk_governance_storage_policies_project
    on governance_storage_policies(project_id);

create table if not exists governance_storage_tags (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    project_id uuid not null references projects(id),
    resource_type varchar(20) not null,
    resource_id uuid not null,
    tag_value varchar(80) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create unique index if not exists uk_governance_storage_tags_resource_tag
    on governance_storage_tags(resource_type, resource_id, tag_value);

create index if not exists idx_governance_storage_tags_project_resource
    on governance_storage_tags(project_id, resource_type, resource_id);

create table if not exists governance_storage_project_metering (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    project_id uuid not null references projects(id),
    bucket_count bigint not null,
    object_count bigint not null,
    stored_bytes bigint not null,
    uploaded_bytes bigint not null,
    downloaded_bytes bigint not null,
    last_recorded_at timestamp not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create unique index if not exists uk_governance_storage_project_metering_project
    on governance_storage_project_metering(project_id);

create table if not exists governance_storage_bucket_metering (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    project_id uuid not null references projects(id),
    bucket_id uuid not null references storage_buckets(id),
    object_count bigint not null,
    stored_bytes bigint not null,
    uploaded_bytes bigint not null,
    downloaded_bytes bigint not null,
    last_recorded_at timestamp not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create unique index if not exists uk_governance_storage_bucket_metering_bucket
    on governance_storage_bucket_metering(bucket_id);
