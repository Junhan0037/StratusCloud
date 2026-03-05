"use client";

import { FormEvent, useState } from "react";
import {
  ApiKeyResponse,
  createApiKey,
  getAuthSession,
  listApiKeys,
  revokeApiKey,
  RoleType
} from "@/shared/lib/api/client";

const roles: RoleType[] = ["OWNER", "ADMIN", "DEVELOPER", "VIEWER"];

// API Key 발급/조회/회수 흐름을 콘솔에서 빠르게 검증하기 위한 패널이다.
export function ApiKeyPanel() {
  const session = getAuthSession();
  const [tenantId, setTenantId] = useState(session.tenantId);
  const [name, setName] = useState("automation-key");
  const [role, setRole] = useState<RoleType>("ADMIN");
  const [projectId, setProjectId] = useState("");
  const [keys, setKeys] = useState<ApiKeyResponse[]>([]);
  const [issuedRawKey, setIssuedRawKey] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  const handleIssue = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setErrorMessage("");
    try {
      const issued = await createApiKey(tenantId.trim(), name.trim(), role, projectId.trim());
      setIssuedRawKey(issued.rawKey ?? "");
      setName("");
      const latest = await listApiKeys(tenantId.trim());
      setKeys(latest);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "API Key 발급에 실패했습니다.");
    }
  };

  const handleRefresh = async () => {
    setErrorMessage("");
    try {
      const latest = await listApiKeys(tenantId.trim());
      setKeys(latest);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "API Key 조회에 실패했습니다.");
    }
  };

  const handleRevoke = async (keyId: string) => {
    setErrorMessage("");
    try {
      await revokeApiKey(keyId);
      const latest = await listApiKeys(tenantId.trim());
      setKeys(latest);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "API Key 회수에 실패했습니다.");
    }
  };

  return (
    <section style={panelStyle}>
      <h2 style={{ marginTop: 0 }}>API Key 관리</h2>
      <form onSubmit={handleIssue} style={{ display: "grid", gap: 10 }}>
        <input
          value={tenantId}
          onChange={(event) => setTenantId(event.target.value)}
          placeholder="Tenant ID(UUID)"
          required
          style={inputStyle}
        />
        <input
          value={projectId}
          onChange={(event) => setProjectId(event.target.value)}
          placeholder="Project ID(UUID, 선택)"
          style={inputStyle}
        />
        <input
          value={name}
          onChange={(event) => setName(event.target.value)}
          placeholder="API Key Name"
          required
          minLength={2}
          style={inputStyle}
        />
        <select value={role} onChange={(event) => setRole(event.target.value as RoleType)} style={inputStyle}>
          {roles.map((item) => (
            <option key={item} value={item}>
              {item}
            </option>
          ))}
        </select>
        <button type="submit" style={buttonStyle}>
          API Key 발급
        </button>
      </form>

      <div style={{ marginTop: 10 }}>
        <button type="button" style={secondaryButtonStyle} onClick={handleRefresh}>
          API Key 목록 새로고침
        </button>
      </div>

      {issuedRawKey ? (
        <p style={infoStyle}>
          방금 발급된 Key(1회 표시): <code>{issuedRawKey}</code>
        </p>
      ) : null}

      <ul style={{ marginTop: 14, paddingLeft: 16 }}>
        {keys.map((key) => (
          <li key={key.id} style={{ marginBottom: 8 }}>
            {key.name} / {key.keyPrefix} / {key.status}
            {key.status === "ACTIVE" ? (
              <button type="button" style={inlineButtonStyle} onClick={() => handleRevoke(key.id)}>
                회수
              </button>
            ) : null}
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
  height: 36,
  border: "1px solid #b5c5dc",
  borderRadius: 10,
  background: "#ffffff",
  color: "#183961",
  fontWeight: 600,
  cursor: "pointer",
  padding: "0 12px"
};

const inlineButtonStyle: React.CSSProperties = {
  marginLeft: 10,
  border: "none",
  borderRadius: 8,
  background: "#e8eefc",
  color: "#0f4dc2",
  fontWeight: 600,
  cursor: "pointer",
  padding: "4px 8px"
};

const infoStyle: React.CSSProperties = {
  marginTop: 10,
  marginBottom: 0,
  fontSize: 12,
  color: "#1f395f"
};

const errorStyle: React.CSSProperties = {
  color: "#d22c2c",
  marginTop: 10,
  marginBottom: 0,
  fontSize: 13
};
