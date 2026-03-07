"use client";

import { FormEvent, useEffect, useState } from "react";
import {
  ComputeHealthStatus,
  ComputeInstanceResponse,
  ProjectResponse,
  getAuthSession,
  listComputeInstances,
  writeComputeInstanceHealth,
  writeComputeInstanceMetric
} from "@/shared/lib/api/client";

interface HealthcheckPanelProps {
  selectedProject: ProjectResponse | null;
}

const healthStatuses: ComputeHealthStatus[] = ["HEALTHY", "UNHEALTHY"];

export function HealthcheckPanel({ selectedProject }: HealthcheckPanelProps) {
  const session = getAuthSession();
  const [tenantId, setTenantId] = useState(session.tenantId);
  const [instances, setInstances] = useState<ComputeInstanceResponse[]>([]);
  const [instanceId, setInstanceId] = useState("");
  const [cpuPercent, setCpuPercent] = useState(85);
  const [memoryPercent, setMemoryPercent] = useState(72);
  const [healthStatus, setHealthStatus] = useState<ComputeHealthStatus>("UNHEALTHY");
  const [detail, setDetail] = useState("probe-timeout");
  const [errorMessage, setErrorMessage] = useState("");

  const refreshInstances = async () => {
    if (!selectedProject) {
      setInstances([]);
      setInstanceId("");
      return;
    }
    try {
      const latest = await listComputeInstances(tenantId.trim(), selectedProject.id);
      setInstances(latest);
      setInstanceId((current) => (current && latest.some((item) => item.id === current) ? current : latest[0]?.id ?? ""));
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "인스턴스 조회에 실패했습니다.");
    }
  };

  useEffect(() => {
    const run = async () => {
      if (!selectedProject) {
        setInstances([]);
        setInstanceId("");
        return;
      }
      try {
        const latest = await listComputeInstances(tenantId.trim(), selectedProject.id);
        setInstances(latest);
        setInstanceId((current) => (current && latest.some((item) => item.id === current) ? current : latest[0]?.id ?? ""));
      } catch (error) {
        setErrorMessage(error instanceof Error ? error.message : "인스턴스 조회에 실패했습니다.");
      }
    };
    void run();
  }, [selectedProject, tenantId]);

  const handleMetric = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!instanceId) {
      setErrorMessage("대상 인스턴스를 먼저 선택하세요.");
      return;
    }
    setErrorMessage("");
    try {
      await writeComputeInstanceMetric(instanceId, cpuPercent, memoryPercent);
      await refreshInstances();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "메트릭 입력에 실패했습니다.");
    }
  };

  const handleHealth = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!instanceId) {
      setErrorMessage("대상 인스턴스를 먼저 선택하세요.");
      return;
    }
    setErrorMessage("");
    try {
      await writeComputeInstanceHealth(instanceId, healthStatus, detail);
      await refreshInstances();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "헬스 상태 입력에 실패했습니다.");
    }
  };

  return (
    <section style={panelStyle}>
      <h2 style={{ marginTop: 0 }}>헬스체크/메트릭</h2>
      <p style={subTextStyle}>
        선택된 프로젝트: {selectedProject ? `${selectedProject.name} (${selectedProject.id.slice(0, 8)})` : "없음"}
      </p>

      <input value={tenantId} onChange={(event) => setTenantId(event.target.value)} placeholder="Tenant ID(UUID)" style={inputStyle} />

      <div style={{ marginTop: 10 }}>
        <select value={instanceId} onChange={(event) => setInstanceId(event.target.value)} style={inputStyle}>
          <option value="">Instance 선택</option>
          {instances.map((instance) => (
            <option key={instance.id} value={instance.id}>
              {instance.name} / {instance.status}
            </option>
          ))}
        </select>
      </div>

      <form onSubmit={handleMetric} style={{ display: "grid", gap: 10, marginTop: 12 }}>
        <div style={gridStyle}>
          <input value={cpuPercent} onChange={(event) => setCpuPercent(Number(event.target.value))} type="number" min={0} max={100} style={inputStyle} />
          <input value={memoryPercent} onChange={(event) => setMemoryPercent(Number(event.target.value))} type="number" min={0} max={100} style={inputStyle} />
        </div>
        <button type="submit" style={buttonStyle}>
          메트릭 입력
        </button>
      </form>

      <form onSubmit={handleHealth} style={{ display: "grid", gap: 10, marginTop: 12 }}>
        <select value={healthStatus} onChange={(event) => setHealthStatus(event.target.value as ComputeHealthStatus)} style={inputStyle}>
          {healthStatuses.map((status) => (
            <option key={status} value={status}>
              {status}
            </option>
          ))}
        </select>
        <input value={detail} onChange={(event) => setDetail(event.target.value)} placeholder="health detail" style={inputStyle} />
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <button type="submit" style={buttonStyle}>
            헬스 상태 입력
          </button>
          <button type="button" style={secondaryButtonStyle} onClick={() => void refreshInstances()}>
            인스턴스 새로고침
          </button>
        </div>
      </form>

      <ul style={{ marginTop: 14, paddingLeft: 16 }}>
        {instances.map((instance) => (
          <li key={instance.id} style={{ marginBottom: 10 }}>
            <strong>{instance.name}</strong> / {instance.status} / {instance.healthStatus}
            <div style={subTextStyle}>
              restart={instance.restartCount} · metric=
              {instance.latestMetric ? `${instance.latestMetric.cpuPercent}%/${instance.latestMetric.memoryPercent}%` : "none"}
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
  padding: "0 12px",
  width: "100%"
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
  cursor: "pointer"
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
