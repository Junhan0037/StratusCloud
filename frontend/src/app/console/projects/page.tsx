"use client";

import { useState } from "react";
import { AutoscalingGroupPanel } from "@/features/compute/autoscaling/AutoscalingGroupPanel";
import { HealthcheckPanel } from "@/features/compute/health/HealthcheckPanel";
import { ImageCatalogPanel } from "@/features/compute/images/ImageCatalogPanel";
import { InstancePanel } from "@/features/compute/instances/InstancePanel";
import { ApiKeyPanel } from "@/features/iam/apikeys/ApiKeyPanel";
import { AuditPanel } from "@/features/iam/audit/AuditPanel";
import { AuthSessionPanel } from "@/features/iam/auth/AuthSessionPanel";
import { MemberPanel } from "@/features/iam/members/MemberPanel";
import { PolicyPanel } from "@/features/iam/policies/PolicyPanel";
import { ProjectPanel } from "@/features/iam/projects/ProjectPanel";
import { SecretPanel } from "@/features/iam/secrets/SecretPanel";
import { ProjectResponse } from "@/shared/lib/api/client";

// Week 6 Compute-2 흐름까지 포함해 한 화면에서 검증하기 위한 콘솔 페이지다.
export default function ConsoleProjectsPage() {
  const [selectedProject, setSelectedProject] = useState<ProjectResponse | null>(null);

  return (
    <main style={{ maxWidth: 1200, margin: "0 auto", padding: "40px 20px" }}>
      <header style={{ marginBottom: 24 }}>
        <h1 style={{ margin: 0 }}>Week 6 Compute Console</h1>
        <p style={{ marginTop: 10, color: "#5d6b7d" }}>
          JWT/API Key 인증, IAM, Secrets, Audit, Compute Image/Instance, Autoscaling/Healthcheck 흐름까지 빠르게 검증할 수 있는 운영 화면입니다.
        </p>
      </header>

      <section
        style={{
          display: "grid",
          gap: 16,
          gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))"
        }}
      >
        <AuthSessionPanel />
        <PolicyPanel />
        <ApiKeyPanel />
        <SecretPanel />
        <AuditPanel />
        <ImageCatalogPanel />
        <AutoscalingGroupPanel selectedProject={selectedProject} />
        <InstancePanel selectedProject={selectedProject} />
        <HealthcheckPanel selectedProject={selectedProject} />
        <ProjectPanel onSelectProject={setSelectedProject} />
        <MemberPanel selectedProject={selectedProject} />
      </section>
    </main>
  );
}
