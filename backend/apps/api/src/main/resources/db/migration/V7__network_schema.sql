-- Week 7에서 사용하는 Network VPC/Subnet/Route/Security Group 스키마를 추가한다.

create table if not exists network_vpcs (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    project_id uuid not null references projects(id),
    name varchar(100) not null,
    cidr_block varchar(32) not null,
    default_route_table_id uuid,
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create unique index if not exists uk_network_vpcs_project_name
    on network_vpcs(project_id, name);

create index if not exists idx_network_vpcs_project_created_at
    on network_vpcs(project_id, created_at desc);

create table if not exists network_route_tables (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    project_id uuid not null references projects(id),
    vpc_id uuid not null references network_vpcs(id),
    name varchar(100) not null,
    is_default boolean not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create unique index if not exists uk_network_route_tables_vpc_name
    on network_route_tables(vpc_id, name);

create index if not exists idx_network_route_tables_vpc_created_at
    on network_route_tables(vpc_id, created_at desc);

create table if not exists network_routes (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    project_id uuid not null references projects(id),
    vpc_id uuid not null references network_vpcs(id),
    route_table_id uuid not null references network_route_tables(id),
    destination_cidr varchar(32) not null,
    target_type varchar(30) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create unique index if not exists uk_network_routes_table_destination
    on network_routes(route_table_id, destination_cidr);

create table if not exists network_subnets (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    project_id uuid not null references projects(id),
    vpc_id uuid not null references network_vpcs(id),
    name varchar(100) not null,
    cidr_block varchar(32) not null,
    availability_zone varchar(40) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create unique index if not exists uk_network_subnets_vpc_name
    on network_subnets(vpc_id, name);

create index if not exists idx_network_subnets_vpc_created_at
    on network_subnets(vpc_id, created_at desc);

create table if not exists network_route_table_associations (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    project_id uuid not null references projects(id),
    route_table_id uuid not null references network_route_tables(id),
    subnet_id uuid not null references network_subnets(id),
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create unique index if not exists uk_network_route_table_associations_subnet
    on network_route_table_associations(subnet_id);

create index if not exists idx_network_route_table_associations_table
    on network_route_table_associations(route_table_id, created_at asc);

create table if not exists network_security_groups (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    project_id uuid not null references projects(id),
    vpc_id uuid not null references network_vpcs(id),
    name varchar(100) not null,
    description varchar(300),
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create unique index if not exists uk_network_security_groups_vpc_name
    on network_security_groups(vpc_id, name);

create index if not exists idx_network_security_groups_project_created_at
    on network_security_groups(project_id, created_at desc);

create table if not exists network_security_group_rules (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    project_id uuid not null references projects(id),
    security_group_id uuid not null references network_security_groups(id),
    direction varchar(20) not null,
    protocol varchar(20) not null,
    port_range_start integer,
    port_range_end integer,
    cidr_block varchar(32) not null,
    description varchar(300),
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create index if not exists idx_network_security_group_rules_group_created_at
    on network_security_group_rules(security_group_id, created_at asc);
