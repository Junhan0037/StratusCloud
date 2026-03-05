"use client";

import { FormEvent, useState } from "react";
import { getAuthSession, setAuthSession } from "@/shared/lib/api/client";

// 운영자가 현재 콘솔 세션의 인증 정보를 명시적으로 제어할 수 있게 만든 패널이다.
export function AuthSessionPanel() {
  const current = getAuthSession();
  const [tenantId, setTenantId] = useState(current.tenantId);
  const [bearerToken, setBearerToken] = useState(current.bearerToken);
  const [apiKey, setApiKey] = useState(current.apiKey);
  const [message, setMessage] = useState("인증 정보가 아직 설정되지 않았습니다.");

  const handleApply = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const next = setAuthSession({
      tenantId: tenantId.trim(),
      bearerToken: bearerToken.trim(),
      apiKey: apiKey.trim()
    });
    const mode = next.bearerToken ? "JWT" : next.apiKey ? "API Key" : "미설정";
    setMessage(`인증 모드: ${mode}`);
  };

  return (
    <section style={panelStyle}>
      <h2 style={{ marginTop: 0 }}>인증 세션</h2>
      <form onSubmit={handleApply} style={{ display: "grid", gap: 10 }}>
        <input
          value={tenantId}
          onChange={(event) => setTenantId(event.target.value)}
          placeholder="Tenant ID (UUID)"
          style={inputStyle}
        />
        <textarea
          value={bearerToken}
          onChange={(event) => setBearerToken(event.target.value)}
          placeholder="JWT Bearer Token (있으면 우선 사용)"
          rows={4}
          style={textareaStyle}
        />
        <input
          value={apiKey}
          onChange={(event) => setApiKey(event.target.value)}
          placeholder="X-API-Key (JWT 미입력 시 사용)"
          style={inputStyle}
        />
        <button type="submit" style={buttonStyle}>
          인증 세션 적용
        </button>
      </form>
      <p style={messageStyle}>{message}</p>
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
  background: "#123f9f",
  color: "#ffffff",
  fontWeight: 600,
  cursor: "pointer"
};

const messageStyle: React.CSSProperties = {
  marginTop: 10,
  marginBottom: 0,
  color: "#1f395f",
  fontSize: 13
};
