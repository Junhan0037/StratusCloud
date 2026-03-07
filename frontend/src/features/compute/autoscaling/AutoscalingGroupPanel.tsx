"use client";

import { FormEvent, useEffect, useState } from "react";
import {
  ComputeAutoscalingGroupResponse,
  ComputeImageResponse,
  ProjectResponse,
  createComputeAutoscalingGroup,
  evaluateComputeAutoscalingGroup,
  getAuthSession,
  listComputeAutoscalingGroups,
  listComputeImages,
  reconcileComputeGroupHealth
} from "@/shared/lib/api/client";

interface AutoscalingGroupPanelProps {
  selectedProject: ProjectResponse | null;
}

const flavors = ["nano", "small", "medium"] as const;

export function AutoscalingGroupPanel({ selectedProject }: AutoscalingGroupPanelProps) {
  const session = getAuthSession();
  const [tenantId, setTenantId] = useState(session.tenantId);
  const [name, setName] = useState("web-asg");
  const [flavor, setFlavor] = useState<(typeof flavors)[number]>("small");
  const [minInstances, setMinInstances] = useState(1);
  const [maxInstances, setMaxInstances] = useState(3);
  const [cpuScaleOut, setCpuScaleOut] = useState(70);
  const [cpuScaleIn, setCpuScaleIn] = useState(25);
  const [memoryScaleOut, setMemoryScaleOut] = useState(80);
  const [memoryScaleIn, setMemoryScaleIn] = useState(30);
  const [failureThreshold, setFailureThreshold] = useState(3);
  const [images, setImages] = useState<ComputeImageResponse[]>([]);
  const [imageId, setImageId] = useState("");
  const [groups, setGroups] = useState<ComputeAutoscalingGroupResponse[]>([]);
  const [errorMessage, setErrorMessage] = useState("");

  const refreshImages = async () => {
    try {
      const latest = await listComputeImages({ tenantId: tenantId.trim(), status: "ACTIVE" });
      setImages(latest);
      setImageId((current) => (current && latest.some((item) => item.id === current) ? current : latest[0]?.id ?? ""));
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "이미지 조회에 실패했습니다.");
    }
  };

  const refreshGroups = async () => {
    if (!selectedProject) {
      setGroups([]);
      return;
    }
    try {
      const latest = await listComputeAutoscalingGroups({
        tenantId: tenantId.trim(),
        projectId: selectedProject.id
      });
      setGroups(latest);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "오토스케일 그룹 조회에 실패했습니다.");
    }
  };

  useEffect(() => {
    const run = async () => {
      try {
        const latest = await listComputeImages({ tenantId: tenantId.trim(), status: "ACTIVE" });
        setImages(latest);
        setImageId((current) => (current && latest.some((item) => item.id === current) ? current : latest[0]?.id ?? ""));
      } catch (error) {
        setErrorMessage(error instanceof Error ? error.message : "이미지 조회에 실패했습니다.");
      }
    };
    void run();
  }, [tenantId]);

  useEffect(() => {
    const run = async () => {
      if (!selectedProject) {
        setGroups([]);
        return;
      }
      try {
        const latest = await listComputeAutoscalingGroups({
          tenantId: tenantId.trim(),
          projectId: selectedProject.id
        });
        setGroups(latest);
      } catch (error) {
        setErrorMessage(error instanceof Error ? error.message : "오토스케일 그룹 조회에 실패했습니다.");
      }
    };
    void run();
  }, [selectedProject, tenantId]);

  const handleCreate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!selectedProject) {
      setErrorMessage("프로젝트를 먼저 선택하세요.");
      return;
    }
    if (!imageId) {
      setErrorMessage("그룹에 사용할 이미지를 선택하세요.");
      return;
    }
    setErrorMessage("");
    try {
      await createComputeAutoscalingGroup(
        tenantId.trim(),
        selectedProject.id,
        name.trim(),
        imageId,
        flavor,
        minInstances,
        maxInstances,
        cpuScaleOut,
        cpuScaleIn,
        memoryScaleOut,
        memoryScaleIn,
        failureThreshold
      );
      setName("worker-asg");
      await refreshGroups();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "오토스케일 그룹 생성에 실패했습니다.");
    }
  };

  const handleGroupAction = async (groupId: string, action: "evaluate" | "reconcile") => {
    setErrorMessage("");
    try {
      if (action === "evaluate") {
        await evaluateComputeAutoscalingGroup(groupId);
      }
      if (action === "reconcile") {
        await reconcileComputeGroupHealth(groupId);
      }
      await refreshGroups();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "오토스케일 그룹 작업에 실패했습니다.");
    }
  };

  return (
    <section style={panelStyle}>
      <h2 style={{ marginTop: 0 }}>오토스케일 그룹</h2>
      <p style={subTextStyle}>
        선택된 프로젝트: {selectedProject ? `${selectedProject.name} (${selectedProject.id.slice(0, 8)})` : "없음"}
      </p>

      <form onSubmit={handleCreate} style={{ display: "grid", gap: 10 }}>
        <input value={tenantId} onChange={(event) => setTenantId(event.target.value)} placeholder="Tenant ID(UUID)" required style={inputStyle} />
        <select value={imageId} onChange={(event) => setImageId(event.target.value)} style={inputStyle}>
          <option value="">Image 선택</option>
          {images.map((image) => (
            <option key={image.id} value={image.id}>
              {image.name} {image.version}
            </option>
          ))}
        </select>
        <input value={name} onChange={(event) => setName(event.target.value)} placeholder="Group Name" required style={inputStyle} />
        <select value={flavor} onChange={(event) => setFlavor(event.target.value as (typeof flavors)[number])} style={inputStyle}>
          {flavors.map((item) => (
            <option key={item} value={item}>
              {item}
            </option>
          ))}
        </select>
        <div style={gridStyle}>
          <input value={minInstances} onChange={(event) => setMinInstances(Number(event.target.value))} type="number" min={1} style={inputStyle} />
          <input value={maxInstances} onChange={(event) => setMaxInstances(Number(event.target.value))} type="number" min={1} style={inputStyle} />
        </div>
        <div style={gridStyle}>
          <input value={cpuScaleOut} onChange={(event) => setCpuScaleOut(Number(event.target.value))} type="number" min={1} max={100} style={inputStyle} />
          <input value={cpuScaleIn} onChange={(event) => setCpuScaleIn(Number(event.target.value))} type="number" min={1} max={100} style={inputStyle} />
        </div>
        <div style={gridStyle}>
          <input value={memoryScaleOut} onChange={(event) => setMemoryScaleOut(Number(event.target.value))} type="number" min={1} max={100} style={inputStyle} />
          <input value={memoryScaleIn} onChange={(event) => setMemoryScaleIn(Number(event.target.value))} type="number" min={1} max={100} style={inputStyle} />
        </div>
        <input value={failureThreshold} onChange={(event) => setFailureThreshold(Number(event.target.value))} type="number" min={1} style={inputStyle} />
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <button type="submit" style={buttonStyle}>
            그룹 생성
          </button>
          <button type="button" style={secondaryButtonStyle} onClick={() => void refreshGroups()}>
            그룹 조회
          </button>
        </div>
      </form>

      <ul style={{ marginTop: 14, paddingLeft: 16 }}>
        {groups.map((group) => (
          <li key={group.id} style={{ marginBottom: 12 }}>
            <strong>{group.name}</strong> / desired {group.desiredInstances} / current {group.currentInstances}
            <div style={subTextStyle}>
              {group.flavor} · cpu {group.averageCpuPercent ?? "-"}% · memory {group.averageMemoryPercent ?? "-"}%
            </div>
            <div style={{ display: "flex", gap: 8, marginTop: 6, flexWrap: "wrap" }}>
              <button type="button" style={inlineButtonStyle} onClick={() => void handleGroupAction(group.id, "evaluate")}>
                Evaluate
              </button>
              <button type="button" style={secondaryButtonStyle} onClick={() => void handleGroupAction(group.id, "reconcile")}>
                Reconcile Health
              </button>
            </div>
          </li>
        ))}
      </ul>

      {errorMessage ? <p style={errorStyle}>{errorMessage}</p> : null}
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

const gridStyle: React.CSSProperties = {
  display: "grid",
  gap: 8,
  gridTemplateColumns: "repeat(2, minmax(0, 1fr))"
};

const buttonStyle: React.CSSProperties = {
  height: 40,
  border: "none",
  borderRadius: 10,
  background: "#0f4dc2",
  color: "#ffffff",
  fontWeight: 600,
  cursor: "pointer",
  padding: "0 12px"
};

const secondaryButtonStyle: React.CSSProperties = {
  height: 40,
  border: "1px solid #b5c5dc",
  borderRadius: 10,
  background: "#ffffff",
  color: "#183961",
  fontWeight: 600,
  cursor: "pointer",
  padding: "0 12px"
};

const inlineButtonStyle: React.CSSProperties = {
  border: "none",
  borderRadius: 8,
  background: "#e8eefc",
  color: "#0f4dc2",
  fontWeight: 600,
  cursor: "pointer",
  padding: "6px 10px"
};

const subTextStyle: React.CSSProperties = {
  marginTop: 4,
  color: "#5d6b7d",
  fontSize: 12
};

const errorStyle: React.CSSProperties = {
  color: "#d22c2c",
  marginTop: 10,
  marginBottom: 0,
  fontSize: 13
};
