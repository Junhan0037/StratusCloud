-- Week 3 IAM-2에서 사용하는 정책/역할 매핑/API Key 스키마를 추가한다.

create table if not exists iam_policies (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    name varchar(100) not null,
    description varchar(300),
    document text not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create unique index if not exists uk_iam_policies_tenant_name on iam_policies(tenant_id, name);

create table if not exists iam_role_policies (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    role varchar(30) not null,
    policy_id uuid not null references iam_policies(id),
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create unique index if not exists uk_iam_role_policies_tenant_role_policy
    on iam_role_policies(tenant_id, role, policy_id);

create table if not exists iam_api_keys (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    project_id uuid references projects(id),
    name varchar(100) not null,
    role varchar(30) not null,
    key_prefix varchar(20) not null,
    secret_hash varchar(128) not null,
    status varchar(20) not null,
    expires_at timestamp not null,
    revoked_at timestamp,
    last_used_at timestamp,
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create unique index if not exists uk_iam_api_keys_prefix on iam_api_keys(key_prefix);
create index if not exists idx_iam_api_keys_tenant_status on iam_api_keys(tenant_id, status);
