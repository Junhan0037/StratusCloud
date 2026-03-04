"use client";

import { useState } from "react";
import { MemberPanel } from "@/features/iam/members/MemberPanel";
import { ProjectPanel } from "@/features/iam/projects/ProjectPanel";
import { ProjectResponse } from "@/shared/lib/api/client";

// 한국어 설명: Week 2 범위의 프로젝트/멤버 관리 기능을 한 화면에서 검증하기 위한 콘솔 페이지다.
export default function ConsoleProjectsPage() {
  const [selectedProject, setSelectedProject] = useState<ProjectResponse | null>(null);

  return (
    <main style={{ maxWidth: 1200, margin: "0 auto", padding: "40px 20px" }}>
      <header style={{ marginBottom: 24 }}>
        <h1 style={{ margin: 0 }}>IAM Week 2 Console</h1>
        <p style={{ marginTop: 10, color: "#5d6b7d" }}>
          프로젝트 생성/조회, 사용자 생성, 멤버 역할 변경을 빠르게 검증할 수 있는 운영 화면입니다.
        </p>
      </header>

      <section
        style={{
          display: "grid",
          gap: 16,
          gridTemplateColumns: "repeat(auto-fit, minmax(320px, 1fr))"
        }}
      >
        <ProjectPanel onSelectProject={setSelectedProject} />
        <MemberPanel selectedProject={selectedProject} />
      </section>
    </main>
  );
}
