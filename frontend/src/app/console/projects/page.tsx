"use client";

import { useState } from "react";
import { AutoscalingGroupPanel } from "@/features/compute/autoscaling/AutoscalingGroupPanel";
import { HealthcheckPanel } from "@/features/compute/health/HealthcheckPanel";
import { ImageCatalogPanel } from "@/features/compute/images/ImageCatalogPanel";
import { InstancePanel } from "@/features/compute/instances/InstancePanel";
import { ManagedDatabasePanel } from "@/features/data/databases/ManagedDatabasePanel";
import { DnsRecordPanel } from "@/features/network/dns/DnsRecordPanel";
import { ElasticIpPanel } from "@/features/network/elastic-ips/ElasticIpPanel";
import { LoadBalancerPanel } from "@/features/network/load-balancers/LoadBalancerPanel";
import { NatGatewayPanel } from "@/features/network/nat/NatGatewayPanel";
import { RouteTablePanel } from "@/features/network/routes/RouteTablePanel";
import { SecurityGroupPanel } from "@/features/network/security/SecurityGroupPanel";
import { SubnetPanel } from "@/features/network/subnets/SubnetPanel";
import { OperationsPanel } from "@/features/system/operations/OperationsPanel";
import { VpcPanel } from "@/features/network/vpcs/VpcPanel";
import { ObjectStoragePanel } from "@/features/storage/object-storage/ObjectStoragePanel";
import { ApiKeyPanel } from "@/features/iam/apikeys/ApiKeyPanel";
import { AuditPanel } from "@/features/iam/audit/AuditPanel";
import { AuthSessionPanel } from "@/features/iam/auth/AuthSessionPanel";
import { MemberPanel } from "@/features/iam/members/MemberPanel";
import { PolicyPanel } from "@/features/iam/policies/PolicyPanel";
import { ProjectPanel } from "@/features/iam/projects/ProjectPanel";
import { SecretPanel } from "@/features/iam/secrets/SecretPanel";
import { ProjectResponse } from "@/shared/lib/api/client";

// Week 11 운영 안정화 흐름까지 한 화면에서 검증하기 위한 콘솔 페이지다.
export default function ConsoleProjectsPage() {
  const [selectedProject, setSelectedProject] = useState<ProjectResponse | null>(null);

  return (
    <div className="space-y-6">
      <div className="pb-5 border-b border-slate-200">
        <h3 className="text-lg leading-6 font-medium text-slate-900">Week 11 Operations Console</h3>
        <p className="mt-2 max-w-4xl text-sm text-slate-500">
          JWT/API Key 인증, IAM, Audit, Managed DB, Compute, Network, Object Storage, Governance, 운영 안정화 흐름까지 빠르게 검증할 수 있는 운영 화면입니다.
        </p>
      </div>

      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        <OperationsPanel />
        <AuthSessionPanel />
        <PolicyPanel />
        <ApiKeyPanel />
        <SecretPanel />
        <AuditPanel />
        <ManagedDatabasePanel selectedProject={selectedProject} />
        <VpcPanel selectedProject={selectedProject} />
        <SubnetPanel selectedProject={selectedProject} />
        <RouteTablePanel selectedProject={selectedProject} />
        <SecurityGroupPanel selectedProject={selectedProject} />
        <LoadBalancerPanel selectedProject={selectedProject} />
        <ElasticIpPanel selectedProject={selectedProject} />
        <NatGatewayPanel selectedProject={selectedProject} />
        <DnsRecordPanel selectedProject={selectedProject} />
        <ObjectStoragePanel selectedProject={selectedProject} />
        <ImageCatalogPanel />
        <AutoscalingGroupPanel selectedProject={selectedProject} />
        <InstancePanel selectedProject={selectedProject} />
        <HealthcheckPanel selectedProject={selectedProject} />
        <ProjectPanel onSelectProject={setSelectedProject} />
        <MemberPanel selectedProject={selectedProject} />
      </div>
    </div>
  );
}
