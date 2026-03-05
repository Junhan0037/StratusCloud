"use client";

import { useState } from "react";
import { ApiKeyPanel } from "@/features/iam/apikeys/ApiKeyPanel";
import { AuthSessionPanel } from "@/features/iam/auth/AuthSessionPanel";
import { MemberPanel } from "@/features/iam/members/MemberPanel";
import { PolicyPanel } from "@/features/iam/policies/PolicyPanel";
import { ProjectPanel } from "@/features/iam/projects/ProjectPanel";
import { ProjectResponse } from "@/shared/lib/api/client";

// Week 3 IAM-2 인증/정책/API Key 시나리오를 한 화면에서 검증하기 위한 콘솔 페이지다.
export default function ConsoleProjectsPage() {
  const [selectedProject, setSelectedProject] = useState<ProjectResponse | null>(null);

  return (
    <main style={{ maxWidth: 1200, margin: "0 auto", padding: "40px 20px" }}>
      <header style={{ marginBottom: 24 }}>
        <h1 style={{ margin: 0 }}>IAM Week 3 Console</h1>
        <p style={{ marginTop: 10, color: "#5d6b7d" }}>
          JWT/API Key 인증, 정책 기반 인가, 프로젝트/멤버 관리 흐름을 빠르게 검증할 수 있는 운영 화면입니다.
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
        <ProjectPanel onSelectProject={setSelectedProject} />
        <MemberPanel selectedProject={selectedProject} />
      </section>
    </main>
  );
}
