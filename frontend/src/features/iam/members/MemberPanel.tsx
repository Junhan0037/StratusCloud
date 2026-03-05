"use client";

import { FormEvent, useMemo, useState } from "react";
import {
  addMember,
  createUser,
  MembershipResponse,
  ProjectResponse,
  RoleType,
  updateMemberRole,
  UserResponse
} from "@/shared/lib/api/client";

interface MemberPanelProps {
  selectedProject: ProjectResponse | null;
}

const roles: RoleType[] = ["OWNER", "ADMIN", "DEVELOPER", "VIEWER"];

// 사용자 생성과 멤버 역할 변경 플로우를 한 화면에서 검증할 수 있게 구성한다.
export function MemberPanel({ selectedProject }: MemberPanelProps) {
  const [email, setEmail] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [selectedRole, setSelectedRole] = useState<RoleType>("DEVELOPER");
  const [lastUser, setLastUser] = useState<UserResponse | null>(null);
  const [lastMembership, setLastMembership] = useState<MembershipResponse | null>(null);
  const [errorMessage, setErrorMessage] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const canSubmit = useMemo(() => Boolean(selectedProject), [selectedProject]);

  const handleCreateAndAssign = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!selectedProject) {
      setErrorMessage("먼저 프로젝트를 선택해주세요.");
      return;
    }

    setErrorMessage("");
    setIsLoading(true);
    try {
      const createdUser = await createUser(email.trim(), displayName.trim());
      const membership = await addMember(selectedProject.id, createdUser.id, selectedRole);
      setLastUser(createdUser);
      setLastMembership(membership);
      setEmail("");
      setDisplayName("");
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "멤버 등록에 실패했습니다.");
    } finally {
      setIsLoading(false);
    }
  };

  const handlePromoteToAdmin = async () => {
    if (!selectedProject || !lastUser) {
      setErrorMessage("역할 변경 대상이 없습니다.");
      return;
    }

    setErrorMessage("");
    setIsLoading(true);
    try {
      const updated = await updateMemberRole(selectedProject.id, lastUser.id, "ADMIN");
      setLastMembership(updated);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "역할 변경에 실패했습니다.");
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <section style={panelStyle}>
      <h2 style={{ marginTop: 0 }}>멤버 관리</h2>
      <p style={{ marginTop: 0, color: "#5d6b7d", fontSize: 13 }}>
        선택된 프로젝트: {selectedProject ? `${selectedProject.name} (${selectedProject.id.slice(0, 8)})` : "없음"}
      </p>

      <form onSubmit={handleCreateAndAssign} style={{ display: "grid", gap: 10 }}>
        <input
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          type="email"
          placeholder="Email"
          required
          style={inputStyle}
          disabled={!canSubmit || isLoading}
        />
        <input
          value={displayName}
          onChange={(event) => setDisplayName(event.target.value)}
          placeholder="Display Name"
          required
          minLength={2}
          style={inputStyle}
          disabled={!canSubmit || isLoading}
        />
        <select
          value={selectedRole}
          onChange={(event) => setSelectedRole(event.target.value as RoleType)}
          style={inputStyle}
          disabled={!canSubmit || isLoading}
        >
          {roles.map((role) => (
            <option key={role} value={role}>
              {role}
            </option>
          ))}
        </select>
        <button type="submit" style={primaryButtonStyle} disabled={!canSubmit || isLoading}>
          {isLoading ? "처리 중..." : "사용자 생성 + 멤버 추가"}
        </button>
      </form>

      <div style={{ marginTop: 14, display: "flex", gap: 8 }}>
        <button type="button" style={secondaryButtonStyle} disabled={!lastUser || isLoading} onClick={handlePromoteToAdmin}>
          마지막 멤버를 ADMIN으로 변경
        </button>
      </div>

      {lastMembership ? (
        <p style={{ marginBottom: 0, marginTop: 12, fontSize: 13, color: "#1f395f" }}>
          마지막 멤버 상태: {lastMembership.userId.slice(0, 8)} / 역할 {lastMembership.role}
        </p>
      ) : null}
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

const primaryButtonStyle: React.CSSProperties = {
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
