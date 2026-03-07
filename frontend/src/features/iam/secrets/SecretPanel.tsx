"use client";

import { FormEvent, useState } from "react";
import {
  createSecret,
  getAuthSession,
  listSecrets,
  listSecretVersions,
  revokeSecretVersion,
  rotateSecret,
  SecretResponse,
  SecretVersionResponse
} from "@/shared/lib/api/client";

export function SecretPanel() {
  const session = getAuthSession();
  const [tenantId, setTenantId] = useState(session.tenantId);
  const [projectId, setProjectId] = useState("");
  const [name, setName] = useState("db-password");
  const [value, setValue] = useState("super-secret-value");
  const [rotateValue, setRotateValue] = useState("rotated-secret-value");
  const [secrets, setSecrets] = useState<SecretResponse[]>([]);
  const [selectedSecretId, setSelectedSecretId] = useState("");
  const [versions, setVersions] = useState<SecretVersionResponse[]>([]);
  const [revealedValue, setRevealedValue] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  const refreshSecrets = async () => {
    const latest = await listSecrets(tenantId.trim(), projectId.trim());
    setSecrets(latest);
    const nextSelectedId = latest.some((item) => item.id === selectedSecretId) ? selectedSecretId : latest[0]?.id ?? "";
    setSelectedSecretId(nextSelectedId);
    if (nextSelectedId) {
      const latestVersions = await listSecretVersions(nextSelectedId);
      setVersions(latestVersions);
    } else {
      setVersions([]);
    }
  };

  const handleCreate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setErrorMessage("");
    try {
      const created = await createSecret(tenantId.trim(), name.trim(), value, projectId.trim());
      setRevealedValue(created.currentVersion?.value ?? "");
      setName("");
      await refreshSecrets();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Secret 생성에 실패했습니다.");
    }
  };

  const handleRotate = async () => {
    if (!selectedSecretId) {
      setErrorMessage("회전할 Secret을 먼저 선택하세요.");
      return;
    }
    setErrorMessage("");
    try {
      const rotated = await rotateSecret(selectedSecretId, rotateValue);
      setRevealedValue(rotated.currentVersion?.value ?? "");
      const latestVersions = await listSecretVersions(selectedSecretId);
      setVersions(latestVersions);
      await refreshSecrets();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Secret 회전에 실패했습니다.");
    }
  };

  const handleSelectSecret = async (secretId: string) => {
    setSelectedSecretId(secretId);
    setErrorMessage("");
    try {
      const latestVersions = await listSecretVersions(secretId);
      setVersions(latestVersions);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Secret 버전 조회에 실패했습니다.");
    }
  };

  const handleRevokeVersion = async (versionId: string) => {
    if (!selectedSecretId) {
      return;
    }
    setErrorMessage("");
    try {
      await revokeSecretVersion(selectedSecretId, versionId);
      const latestVersions = await listSecretVersions(selectedSecretId);
      setVersions(latestVersions);
      await refreshSecrets();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Secret 버전 회수에 실패했습니다.");
    }
  };

  return (
    <section style={panelStyle}>
      <h2 style={{ marginTop: 0 }}>Secrets 관리</h2>
      <form onSubmit={handleCreate} style={{ display: "grid", gap: 10 }}>
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
          placeholder="Secret Name"
          required
          minLength={2}
          style={inputStyle}
        />
        <textarea
          value={value}
          onChange={(event) => setValue(event.target.value)}
          placeholder="Secret Value"
          required
          rows={3}
          style={textareaStyle}
        />
        <button type="submit" style={buttonStyle}>
          Secret 생성
        </button>
      </form>

      <div style={{ display: "flex", gap: 8, marginTop: 12, flexWrap: "wrap" }}>
        <button type="button" style={secondaryButtonStyle} onClick={() => void refreshSecrets()}>
          Secret 목록 새로고침
        </button>
        <button type="button" style={secondaryButtonStyle} onClick={() => void handleRotate()}>
          선택 Secret 회전
        </button>
      </div>

      <textarea
        value={rotateValue}
        onChange={(event) => setRotateValue(event.target.value)}
        placeholder="Rotate Value"
        rows={2}
        style={{ ...textareaStyle, marginTop: 10 }}
      />

      {revealedValue ? (
        <p style={infoStyle}>
          이번 응답에서만 노출된 값: <code>{revealedValue}</code>
        </p>
      ) : null}

      <div style={{ marginTop: 14 }}>
        <strong style={{ display: "block", marginBottom: 8 }}>Secrets</strong>
        <ul style={{ margin: 0, paddingLeft: 16 }}>
          {secrets.map((secret) => (
            <li key={secret.id} style={{ marginBottom: 8 }}>
              <button type="button" style={linkButtonStyle} onClick={() => void handleSelectSecret(secret.id)}>
                {secret.name}
              </button>
              {" / "}v{secret.latestVersion}
            </li>
          ))}
        </ul>
      </div>

      <div style={{ marginTop: 14 }}>
        <strong style={{ display: "block", marginBottom: 8 }}>Selected Versions</strong>
        <ul style={{ margin: 0, paddingLeft: 16 }}>
          {versions.map((version) => (
            <li key={version.id} style={{ marginBottom: 8 }}>
              v{version.version} / {version.status}
              {version.status === "ACTIVE" ? (
                <button type="button" style={inlineButtonStyle} onClick={() => void handleRevokeVersion(version.id)}>
                  회수
                </button>
              ) : null}
            </li>
          ))}
        </ul>
      </div>

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

const linkButtonStyle: React.CSSProperties = {
  border: "none",
  background: "transparent",
  color: "#0f4dc2",
  cursor: "pointer",
  padding: 0,
  fontWeight: 600
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
