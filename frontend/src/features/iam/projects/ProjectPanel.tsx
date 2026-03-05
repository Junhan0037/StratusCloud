"use client";

import { FormEvent, useState } from "react";
import { createProject, ProjectResponse } from "@/shared/lib/api/client";

interface ProjectPanelProps {
  onSelectProject: (project: ProjectResponse) => void;
}

// 프로젝트 생성과 선택 흐름을 분리해 Week 2 콘솔 뼈대를 빠르게 구성한다.
export function ProjectPanel({ onSelectProject }: ProjectPanelProps) {
  const [tenantId, setTenantId] = useState("");
  const [projectName, setProjectName] = useState("");
  const [projects, setProjects] = useState<ProjectResponse[]>([]);
  const [errorMessage, setErrorMessage] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleCreateProject = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setErrorMessage("");
    setIsSubmitting(true);

    try {
      const created = await createProject(tenantId.trim(), projectName.trim());
      setProjects((prev) => [created, ...prev]);
      setProjectName("");
      onSelectProject(created);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "프로젝트 생성에 실패했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <section style={panelStyle}>
      <h2 style={{ marginTop: 0 }}>프로젝트 관리</h2>
      <form onSubmit={handleCreateProject} style={{ display: "grid", gap: 10 }}>
        <input
          value={tenantId}
          onChange={(event) => setTenantId(event.target.value)}
          placeholder="Tenant ID(UUID)"
          required
          style={inputStyle}
        />
        <input
          value={projectName}
          onChange={(event) => setProjectName(event.target.value)}
          placeholder="Project Name"
          required
          minLength={2}
          style={inputStyle}
        />
        <button disabled={isSubmitting} type="submit" style={buttonStyle}>
          {isSubmitting ? "생성 중..." : "프로젝트 생성"}
        </button>
      </form>
      {errorMessage ? <p style={errorStyle}>{errorMessage}</p> : null}

      <ul style={{ marginTop: 16, paddingLeft: 16 }}>
        {projects.map((project) => (
          <li key={project.id} style={{ marginBottom: 8 }}>
            <button type="button" onClick={() => onSelectProject(project)} style={listButtonStyle}>
              {project.name} ({project.id.slice(0, 8)})
            </button>
          </li>
        ))}
      </ul>
    </section>
  );
}

const panelStyle: React.CSSProperties = {
  background: "#ffffff",
  borderRadius: 16,
  padding: 20,
  boxShadow: "0 10px 20px rgba(11, 34, 68, 0.08)"
};

const inputStyle: React.CSSProperties = {
  height: 40,
  border: "1px solid #d8e1ee",
  borderRadius: 10,
  padding: "0 12px"
};

const buttonStyle: React.CSSProperties = {
  height: 40,
  border: "none",
  borderRadius: 10,
  background: "#0f4dc2",
  color: "#ffffff",
  fontWeight: 600,
  cursor: "pointer"
};

const listButtonStyle: React.CSSProperties = {
  border: "none",
  background: "transparent",
  color: "#0f4dc2",
  cursor: "pointer",
  padding: 0,
  fontSize: 14
};

const errorStyle: React.CSSProperties = {
  color: "#d22c2c",
  marginTop: 10,
  marginBottom: 0,
  fontSize: 13
};
