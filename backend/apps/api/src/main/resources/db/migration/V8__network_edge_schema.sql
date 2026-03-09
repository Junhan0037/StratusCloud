-- Week 8에서 사용하는 Load Balancer, Elastic IP, NAT Gateway, DNS 스키마를 추가한다.

alter table network_routes
    add column if not exists target_resource_id uuid;

create table if not exists network_load_balancers (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    project_id uuid not null references projects(id),
    vpc_id uuid not null references network_vpcs(id),
    name varchar(100) not null,
    type varchar(20) not null,
    scheme varchar(30) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create unique index if not exists uk_network_load_balancers_vpc_name
    on network_load_balancers(vpc_id, name);

create index if not exists idx_network_load_balancers_project_created_at
    on network_load_balancers(project_id, created_at desc);

create table if not exists network_load_balancer_listeners (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    project_id uuid not null references projects(id),
    load_balancer_id uuid not null references network_load_balancers(id),
    protocol varchar(20) not null,
    port integer not null,
    default_target_subnet_id uuid not null references network_subnets(id),
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create unique index if not exists uk_network_load_balancer_listeners_lb_port
    on network_load_balancer_listeners(load_balancer_id, port);

create index if not exists idx_network_load_balancer_listeners_lb_created_at
    on network_load_balancer_listeners(load_balancer_id, created_at asc);

create table if not exists network_load_balancer_rules (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    project_id uuid not null references projects(id),
    listener_id uuid not null references network_load_balancer_listeners(id),
    priority integer not null,
    path_pattern varchar(200) not null,
    target_subnet_id uuid not null references network_subnets(id),
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create unique index if not exists uk_network_load_balancer_rules_listener_priority
    on network_load_balancer_rules(listener_id, priority);

create table if not exists network_elastic_ips (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    project_id uuid not null references projects(id),
    name varchar(100) not null,
    public_ip varchar(30) not null,
    allocation_status varchar(20) not null,
    attachment_target_type varchar(30),
    attachment_target_id uuid,
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create unique index if not exists uk_network_elastic_ips_project_name
    on network_elastic_ips(project_id, name);

create unique index if not exists uk_network_elastic_ips_public_ip
    on network_elastic_ips(public_ip);

create index if not exists idx_network_elastic_ips_project_created_at
    on network_elastic_ips(project_id, created_at desc);

create table if not exists network_nat_gateways (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    project_id uuid not null references projects(id),
    vpc_id uuid not null references network_vpcs(id),
    subnet_id uuid not null references network_subnets(id),
    name varchar(100) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create unique index if not exists uk_network_nat_gateways_vpc_name
    on network_nat_gateways(vpc_id, name);

create unique index if not exists uk_network_nat_gateways_subnet
    on network_nat_gateways(subnet_id);

create index if not exists idx_network_nat_gateways_project_created_at
    on network_nat_gateways(project_id, created_at desc);

create table if not exists network_dns_records (
    id uuid primary key,
    tenant_id uuid not null references tenants(id),
    project_id uuid not null references projects(id),
    name varchar(200) not null,
    record_type varchar(10) not null,
    target_type varchar(30) not null,
    target_id uuid not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    created_by varchar(100) not null
);

create unique index if not exists uk_network_dns_records_project_name
    on network_dns_records(project_id, name);

create index if not exists idx_network_dns_records_project_created_at
    on network_dns_records(project_id, created_at desc);
