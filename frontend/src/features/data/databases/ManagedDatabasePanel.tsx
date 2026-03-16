"use client";

import { FormEvent, useEffect, useState } from "react";
import { buttonStyle, dangerButtonStyle, errorStyle, inputStyle, panelStyle, secondaryButtonStyle, subTextStyle } from "@/features/network/shared/panelStyles";
import {
  createManagedDatabase,
  deleteManagedDatabase,
  getAuthSession,
  listManagedDatabases,
  ManagedDatabaseResponse,
  ProjectResponse
} from "@/shared/lib/api/client";

interface ManagedDatabasePanelProps {
  selectedProject: ProjectResponse | null;
}

export function ManagedDatabasePanel({ selectedProject }: ManagedDatabasePanelProps) {
  const session = getAuthSession();
  const [tenantId, setTenantId] = useState(session.tenantId);
  const [name, setName] = useState("orders-db");
  const [engineVersion, setEngineVersion] = useState("16.2");
  const [instanceClass, setInstanceClass] = useState("db-small");
  const [storageGb, setStorageGb] = useState("20");
  const [databases, setDatabases] = useState<ManagedDatabaseResponse[]>([]);
  const [errorMessage, setErrorMessage] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const refresh = async () => {
    if (!selectedProject) {
      setDatabases([]);
      return;
    }
    const latest = await listManagedDatabases(tenantId.trim(), selectedProject.id);
    setDatabases(latest);
  };

  useEffect(() => {
    const run = async () => {
      try {
        if (!selectedProject) {
          setDatabases([]);
          return;
        }
        const latest = await listManagedDatabases(tenantId.trim(), selectedProject.id);
        setDatabases(latest);
      } catch (error) {
        setErrorMessage(error instanceof Error ? error.message : "Managed DB 조회에 실패했습니다.");
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
    setErrorMessage("");
    setIsSubmitting(true);
    try {
      await createManagedDatabase(
        tenantId.trim(),
        selectedProject.id,
        name.trim(),
        engineVersion.trim(),
        instanceClass.trim(),
        Number(storageGb)
      );
      setName("analytics-db");
      await refresh();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Managed DB 생성에 실패했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (databaseId: string) => {
    setErrorMessage("");
    try {
      await deleteManagedDatabase(databaseId);
      await refresh();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Managed DB 삭제에 실패했습니다.");
    }
  };

  return (
    <section style={panelStyle}>
      <h2 style={{ marginTop: 0 }}>Managed DB</h2>
      <p style={subTextStyle}>
        PostgreSQL 제어면 리소스를 생성하고 상태를 확인합니다.
      </p>

      <form onSubmit={handleCreate} style={{ display: "grid", gap: 10 }}>
        <input
          value={tenantId}
          onChange={(event) => setTenantId(event.target.value)}
          placeholder="Tenant ID(UUID)"
          required
          style={inputStyle}
        />
        <input
          value={selectedProject?.name ?? ""}
          placeholder="Selected Project"
          disabled
          style={{ ...inputStyle, background: "#f8fbff", color: "#5d6b7d" }}
        />
        <input value={name} onChange={(event) => setName(event.target.value)} placeholder="Database Name" required style={inputStyle} />
        <input
          value={engineVersion}
          onChange={(event) => setEngineVersion(event.target.value)}
          placeholder="Engine Version"
          required
          style={inputStyle}
        />
        <input
          value={instanceClass}
          onChange={(event) => setInstanceClass(event.target.value)}
          placeholder="Instance Class"
          required
          style={inputStyle}
        />
        <input
          value={storageGb}
          onChange={(event) => setStorageGb(event.target.value)}
          placeholder="Storage GB"
          required
          inputMode="numeric"
          style={inputStyle}
        />
        <button type="submit" disabled={isSubmitting || !selectedProject} style={buttonStyle}>
          {isSubmitting ? "생성 중..." : "Managed DB 생성"}
        </button>
      </form>

      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginTop: 12 }}>
        <strong>리소스 목록</strong>
        <button type="button" style={secondaryButtonStyle} onClick={() => void refresh()} disabled={!selectedProject}>
          새로고침
        </button>
      </div>

      <ul style={{ marginTop: 12, paddingLeft: 16 }}>
        {databases.map((database) => (
          <li key={database.id} style={{ marginBottom: 12 }}>
            <div style={{ display: "flex", justifyContent: "space-between", gap: 12, alignItems: "center" }}>
              <div>
                <strong>{database.name}</strong> · {database.engine} {database.engineVersion}
                <div style={subTextStyle}>
                  {database.instanceClass} / {database.storageGb}GB / {database.status}
                </div>
                <div style={{ ...subTextStyle, fontSize: 12 }}>{database.id.slice(0, 8)}</div>
              </div>
              <button type="button" style={dangerButtonStyle} onClick={() => void handleDelete(database.id)}>
                삭제
              </button>
            </div>
          </li>
        ))}
      </ul>

      {!selectedProject ? <p style={subTextStyle}>프로젝트를 먼저 선택해야 생성과 조회가 가능합니다.</p> : null}
      {errorMessage ? <p style={errorStyle}>{errorMessage}</p> : null}
    </section>
  );
}
