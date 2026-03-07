-- Week 4에서 사용하는 Audit Event 스키마를 추가한다.

create table if not exists audit_events (
    id uuid primary key,
    trace_id varchar(64) not null,
    actor_id uuid not null,
    tenant_id uuid not null,
    project_id uuid,
    action varchar(100) not null,
    resource_type varchar(40) not null,
    resource_id varchar(120),
    result varchar(20) not null,
    metadata text not null,
    occurred_at timestamp not null
);

create index if not exists idx_audit_events_tenant_occurred_at on audit_events(tenant_id, occurred_at desc);
create index if not exists idx_audit_events_action_result on audit_events(action, result);
