-- Week 9 Object Storage용 버킷, 오브젝트, presigned token 스키마를 추가한다.

create table if not exists storage_buckets (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    project_id uuid not null references projects(id),
    name varchar(100) not null,
    acl varchar(30) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create unique index if not exists uk_storage_buckets_project_name
    on storage_buckets(project_id, name);

create index if not exists idx_storage_buckets_project_created_at
    on storage_buckets(project_id, created_at desc);

create table if not exists storage_objects (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    project_id uuid not null references projects(id),
    bucket_id uuid not null references storage_buckets(id),
    object_key varchar(300) not null,
    content_type varchar(120) not null,
    size_bytes bigint not null,
    etag varchar(80) not null,
    acl varchar(30) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create unique index if not exists uk_storage_objects_bucket_key
    on storage_objects(bucket_id, object_key);

create index if not exists idx_storage_objects_bucket_created_at
    on storage_objects(bucket_id, created_at desc);

create table if not exists storage_presigned_tokens (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    project_id uuid not null references projects(id),
    bucket_id uuid not null references storage_buckets(id),
    operation varchar(20) not null,
    object_key varchar(300) not null,
    content_type varchar(120),
    acl varchar(30) not null,
    expires_at timestamp not null,
    used_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create index if not exists idx_storage_presigned_tokens_bucket_created_at
    on storage_presigned_tokens(bucket_id, created_at desc);
