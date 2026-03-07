"use client";

import { FormEvent, useEffect, useState } from "react";
import {
  ComputeImageResponse,
  ComputeInstanceResponse,
  ProjectResponse,
  createComputeInstance,
  getAuthSession,
  listComputeImages,
  listComputeInstances,
  startComputeInstance,
  stopComputeInstance,
  terminateComputeInstance
} from "@/shared/lib/api/client";

interface InstancePanelProps {
  selectedProject: ProjectResponse | null;
}

const flavors = ["nano", "small", "medium"] as const;

export function InstancePanel({ selectedProject }: InstancePanelProps) {
  const session = getAuthSession();
  const [tenantId, setTenantId] = useState(session.tenantId);
  const [name, setName] = useState("web-01");
  const [flavor, setFlavor] = useState<(typeof flavors)[number]>("small");
  const [userData, setUserData] = useState("#!/bin/bash\necho ready");
  const [images, setImages] = useState<ComputeImageResponse[]>([]);
  const [imageId, setImageId] = useState("");
  const [instances, setInstances] = useState<ComputeInstanceResponse[]>([]);
  const [errorMessage, setErrorMessage] = useState("");

  const refreshImages = async () => {
    try {
      const latestImages = await listComputeImages({ tenantId: tenantId.trim(), status: "ACTIVE" });
      setImages(latestImages);
      setImageId((current) => (current && latestImages.some((item) => item.id === current) ? current : latestImages[0]?.id ?? ""));
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "이미지 조회에 실패했습니다.");
    }
  };

  const refreshInstances = async () => {
    if (!selectedProject) {
      setInstances([]);
      return;
    }
    try {
      const latest = await listComputeInstances(tenantId.trim(), selectedProject.id);
      setInstances(latest);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "인스턴스 조회에 실패했습니다.");
    }
  };

  useEffect(() => {
    const run = async () => {
      try {
        const latestImages = await listComputeImages({ tenantId: tenantId.trim(), status: "ACTIVE" });
        setImages(latestImages);
        setImageId((current) => (current && latestImages.some((item) => item.id === current) ? current : latestImages[0]?.id ?? ""));
      } catch (error) {
        setErrorMessage(error instanceof Error ? error.message : "이미지 조회에 실패했습니다.");
      }
    };
    void run();
  }, [tenantId]);

  useEffect(() => {
    const run = async () => {
      if (!selectedProject) {
        setInstances([]);
        return;
      }
      try {
        const latest = await listComputeInstances(tenantId.trim(), selectedProject.id);
        setInstances(latest);
      } catch (error) {
        setErrorMessage(error instanceof Error ? error.message : "인스턴스 조회에 실패했습니다.");
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
      setErrorMessage("인스턴스에 사용할 이미지를 먼저 조회하세요.");
      return;
    }
    setErrorMessage("");
    try {
      await createComputeInstance(tenantId.trim(), selectedProject.id, name.trim(), imageId, flavor, userData);
      setName("");
      await refreshInstances();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "인스턴스 생성에 실패했습니다.");
    }
  };

  const handleAction = async (instanceId: string, action: "start" | "stop" | "terminate") => {
    setErrorMessage("");
    try {
      if (action === "start") {
        await startComputeInstance(instanceId);
      }
      if (action === "stop") {
        await stopComputeInstance(instanceId);
      }
      if (action === "terminate") {
        await terminateComputeInstance(instanceId);
      }
      await refreshInstances();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "인스턴스 제어에 실패했습니다.");
    }
  };

  return (
    <section style={panelStyle}>
      <h2 style={{ marginTop: 0 }}>인스턴스 운영</h2>
      <p style={subTextStyle}>
        선택된 프로젝트: {selectedProject ? `${selectedProject.name} (${selectedProject.id.slice(0, 8)})` : "없음"}
      </p>

      <form onSubmit={handleCreate} style={{ display: "grid", gap: 10 }}>
        <input
          value={tenantId}
          onChange={(event) => setTenantId(event.target.value)}
          placeholder="Tenant ID(UUID)"
          required
          style={inputStyle}
        />
        <select value={imageId} onChange={(event) => setImageId(event.target.value)} style={inputStyle}>
          <option value="">Image 선택</option>
          {images.map((image) => (
            <option key={image.id} value={image.id}>
              {image.name} {image.version}
            </option>
          ))}
        </select>
        <input value={name} onChange={(event) => setName(event.target.value)} placeholder="Instance Name" required style={inputStyle} />
        <select value={flavor} onChange={(event) => setFlavor(event.target.value as (typeof flavors)[number])} style={inputStyle}>
          {flavors.map((item) => (
            <option key={item} value={item}>
              {item}
            </option>
          ))}
        </select>
        <textarea
          value={userData}
          onChange={(event) => setUserData(event.target.value)}
          placeholder="userData"
          rows={3}
          style={textareaStyle}
        />
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <button type="submit" style={buttonStyle}>
            인스턴스 생성
          </button>
          <button type="button" style={secondaryButtonStyle} onClick={() => void refreshImages()}>
            이미지 새로고침
          </button>
          <button type="button" style={secondaryButtonStyle} onClick={() => void refreshInstances()}>
            인스턴스 조회
          </button>
        </div>
      </form>

      <ul style={{ marginTop: 14, paddingLeft: 16 }}>
        {instances.map((instance) => (
          <li key={instance.id} style={{ marginBottom: 12 }}>
            <strong>{instance.name}</strong> / {instance.flavor} / {instance.status}
            <div style={subTextStyle}>
              image={instance.imageId.slice(0, 8)} · group={instance.autoscalingGroupId?.slice(0, 8) ?? "manual"} · lastTransition={instance.lastTransitionAt}
            </div>
            <div style={subTextStyle}>
              health={instance.healthStatus} · restart={instance.restartCount} · metric=
              {instance.latestMetric ? `${instance.latestMetric.cpuPercent}%/${instance.latestMetric.memoryPercent}%` : "none"}
            </div>
            <div style={{ display: "flex", gap: 8, marginTop: 6, flexWrap: "wrap" }}>
              <button type="button" style={inlineButtonStyle} onClick={() => void handleAction(instance.id, "start")}>
                Start
              </button>
              <button type="button" style={inlineButtonStyle} onClick={() => void handleAction(instance.id, "stop")}>
                Stop
              </button>
              <button type="button" style={dangerButtonStyle} onClick={() => void handleAction(instance.id, "terminate")}>
                Terminate
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

const textareaStyle: React.CSSProperties = {
  border: "1px solid #d8e1ee",
  borderRadius: 10,
  padding: "10px 12px",
  resize: "vertical"
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

const dangerButtonStyle: React.CSSProperties = {
  border: "none",
  borderRadius: 8,
  background: "#fde8e8",
  color: "#c62828",
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
