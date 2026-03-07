"use client";

import { FormEvent, useState } from "react";
import { AuditLogResponse, AuditResult, getAuthSession, listAuditLogs } from "@/shared/lib/api/client";

const auditResults: Array<AuditResult | ""> = ["", "SUCCESS", "DENIED"];

export function AuditPanel() {
  const session = getAuthSession();
  const [tenantId, setTenantId] = useState(session.tenantId);
  const [action, setAction] = useState("");
  const [resourceType, setResourceType] = useState("");
  const [result, setResult] = useState<AuditResult | "">("");
  const [logs, setLogs] = useState<AuditLogResponse[]>([]);
  const [errorMessage, setErrorMessage] = useState("");

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setErrorMessage("");
    try {
      const latest = await listAuditLogs({
        tenantId: tenantId.trim(),
        action,
        resourceType,
        result
      });
      setLogs(latest);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Audit 로그 조회에 실패했습니다.");
    }
  };

  return (
    <section style={panelStyle}>
      <h2 style={{ marginTop: 0 }}>Audit 로그</h2>
      <form onSubmit={handleSubmit} style={{ display: "grid", gap: 10 }}>
        <input
          value={tenantId}
          onChange={(event) => setTenantId(event.target.value)}
          placeholder="Tenant ID(UUID)"
          required
          style={inputStyle}
        />
        <input
          value={action}
          onChange={(event) => setAction(event.target.value)}
          placeholder="Action (예: iam:secret:create)"
          style={inputStyle}
        />
        <input
          value={resourceType}
          onChange={(event) => setResourceType(event.target.value)}
          placeholder="Resource Type (예: SECRET)"
          style={inputStyle}
        />
        <select value={result} onChange={(event) => setResult(event.target.value as AuditResult | "")} style={inputStyle}>
          {auditResults.map((item) => (
            <option key={item || "ALL"} value={item}>
              {item || "ALL"}
            </option>
          ))}
        </select>
        <button type="submit" style={buttonStyle}>
          Audit 조회
        </button>
      </form>

      <ul style={{ marginTop: 14, paddingLeft: 16 }}>
        {logs.map((log) => (
          <li key={log.id} style={{ marginBottom: 10 }}>
            [{log.result}] {log.action} / {log.resourceType}
            <div style={subTextStyle}>
              actor={log.actorId} traceId={log.traceId}
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

const buttonStyle: React.CSSProperties = {
  height: 40,
  border: "none",
  borderRadius: 10,
  background: "#0f4dc2",
  color: "#ffffff",
  fontWeight: 600,
  cursor: "pointer"
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
