"use client";

import { FormEvent, useState } from "react";
import {
  bindRolePolicies,
  createPolicy,
  getAuthSession,
  listPolicies,
  PolicyResponse,
  RoleType
} from "@/shared/lib/api/client";

const roles: RoleType[] = ["OWNER", "ADMIN", "DEVELOPER", "VIEWER"];

// 정책 생성/조회와 역할 바인딩까지 한 번에 점검하기 위한 운영 패널이다.
export function PolicyPanel() {
  const session = getAuthSession();
  const [tenantId, setTenantId] = useState(session.tenantId);
  const [policyName, setPolicyName] = useState("");
  const [description, setDescription] = useState("");
  const [action, setAction] = useState("iam:policy:list");
  const [resource, setResource] = useState("tenant:*");
  const [bindRole, setBindRole] = useState<RoleType>("DEVELOPER");
  const [policies, setPolicies] = useState<PolicyResponse[]>([]);
  const [errorMessage, setErrorMessage] = useState("");

  const refreshPolicies = async () => {
    const items = await listPolicies(tenantId.trim());
    setPolicies(items);
  };

  const handleCreatePolicy = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setErrorMessage("");
    try {
      const created = await createPolicy(
        tenantId.trim(),
        policyName.trim(),
        description.trim(),
        action.trim(),
        resource.trim()
      );
      setPolicyName("");
      setDescription("");
      await bindRolePolicies(tenantId.trim(), bindRole, [created.id]);
      await refreshPolicies();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "정책 생성에 실패했습니다.");
    }
  };

  const handleRefresh = async () => {
    setErrorMessage("");
    try {
      await refreshPolicies();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "정책 조회에 실패했습니다.");
    }
  };

  return (
    <section style={panelStyle}>
      <h2 style={{ marginTop: 0 }}>정책 관리</h2>
      <form onSubmit={handleCreatePolicy} style={{ display: "grid", gap: 10 }}>
        <input
          value={tenantId}
          onChange={(event) => setTenantId(event.target.value)}
          placeholder="Tenant ID(UUID)"
          required
          style={inputStyle}
        />
        <input
          value={policyName}
          onChange={(event) => setPolicyName(event.target.value)}
          placeholder="Policy Name"
          required
          minLength={2}
          style={inputStyle}
        />
        <input
          value={description}
          onChange={(event) => setDescription(event.target.value)}
          placeholder="Description"
          style={inputStyle}
        />
        <input
          value={action}
          onChange={(event) => setAction(event.target.value)}
          placeholder="Action (예: iam:project:read)"
          required
          style={inputStyle}
        />
        <input
          value={resource}
          onChange={(event) => setResource(event.target.value)}
          placeholder="Resource (예: project:*)"
          required
          style={inputStyle}
        />
        <select value={bindRole} onChange={(event) => setBindRole(event.target.value as RoleType)} style={inputStyle}>
          {roles.map((role) => (
            <option key={role} value={role}>
              {role}
            </option>
          ))}
        </select>
        <button type="submit" style={buttonStyle}>
          정책 생성 + 역할 바인딩
        </button>
      </form>

      <div style={{ marginTop: 10 }}>
        <button type="button" style={secondaryButtonStyle} onClick={handleRefresh}>
          정책 목록 새로고침
        </button>
      </div>

      <ul style={{ marginTop: 14, paddingLeft: 16 }}>
        {policies.map((policy) => (
          <li key={policy.id} style={{ marginBottom: 6 }}>
            {policy.name} / {policy.document.statements[0]?.actions.join(", ") ?? "-"}
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

const errorStyle: React.CSSProperties = {
  color: "#d22c2c",
  marginTop: 10,
  marginBottom: 0,
  fontSize: 13
};
