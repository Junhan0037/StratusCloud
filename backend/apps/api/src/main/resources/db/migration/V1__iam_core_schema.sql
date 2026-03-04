-- 한국어 설명: Week 2에서 필요한 IAM 핵심 테이블을 초기 생성한다.

create table if not exists tenants (
    id uuid primary key,
    name varchar(100) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create unique index if not exists uk_tenants_name on tenants(name);

create table if not exists projects (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    name varchar(100) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create unique index if not exists uk_projects_tenant_name on projects(tenant_id, name);

create table if not exists iam_users (
    id uuid primary key,
    email varchar(255) not null,
    display_name varchar(100) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create unique index if not exists uk_users_email on iam_users(email);

create table if not exists project_memberships (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    project_id uuid not null references projects(id),
    user_id uuid not null references iam_users(id),
    role varchar(30) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create unique index if not exists uk_memberships_project_user on project_memberships(project_id, user_id);
