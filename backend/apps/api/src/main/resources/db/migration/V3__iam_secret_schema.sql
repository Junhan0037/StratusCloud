-- Week 4에서 사용하는 Secret/SecretVersion 스키마를 추가한다.

create table if not exists iam_secrets (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    project_id uuid references projects(id),
    name varchar(100) not null,
    latest_version integer not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create index if not exists idx_iam_secrets_tenant_project on iam_secrets(tenant_id, project_id);

create table if not exists iam_secret_versions (
    id uuid primary key,
    secret_id uuid not null references iam_secrets(id),
    version integer not null,
    value_ciphertext text not null,
    status varchar(20) not null,
    revoked_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create unique index if not exists uk_iam_secret_versions_secret_version
    on iam_secret_versions(secret_id, version);
