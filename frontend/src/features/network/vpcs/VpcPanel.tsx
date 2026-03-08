"use client";

import { FormEvent, useEffect, useState } from "react";
import { ProjectResponse, VpcResponse, createVpc, deleteVpc, getAuthSession, listVpcs } from "@/shared/lib/api/client";
import { buttonStyle, dangerButtonStyle, errorStyle, inputStyle, panelStyle, secondaryButtonStyle, subTextStyle } from "@/features/network/shared/panelStyles";

interface VpcPanelProps {
  selectedProject: ProjectResponse | null;
}

export function VpcPanel({ selectedProject }: VpcPanelProps) {
  const session = getAuthSession();
  const [tenantId, setTenantId] = useState(session.tenantId);
  const [name, setName] = useState("core-vpc");
  const [cidrBlock, setCidrBlock] = useState("10.0.0.0/16");
  const [vpcs, setVpcs] = useState<VpcResponse[]>([]);
  const [errorMessage, setErrorMessage] = useState("");

  const refresh = async () => {
    if (!selectedProject) {
      setVpcs([]);
      return;
    }
    try {
      const latest = await listVpcs(tenantId.trim(), selectedProject.id);
      setVpcs(latest);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "VPC 조회에 실패했습니다.");
    }
  };

  useEffect(() => {
    const run = async () => {
      try {
        if (!selectedProject) {
          setVpcs([]);
          return;
        }
        const latest = await listVpcs(tenantId.trim(), selectedProject.id);
        setVpcs(latest);
      } catch (error) {
        setErrorMessage(error instanceof Error ? error.message : "VPC 조회에 실패했습니다.");
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
    try {
      await createVpc(tenantId.trim(), selectedProject.id, name.trim(), cidrBlock.trim());
      setName("service-vpc");
      await refresh();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "VPC 생성에 실패했습니다.");
    }
  };

  const handleDelete = async (vpcId: string) => {
    setErrorMessage("");
    try {
      await deleteVpc(vpcId);
      await refresh();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "VPC 삭제에 실패했습니다.");
    }
  };

  return (
    <section style={panelStyle}>
      <h2 style={{ marginTop: 0 }}>VPC 운영</h2>
      <p style={subTextStyle}>
        선택된 프로젝트: {selectedProject ? `${selectedProject.name} (${selectedProject.id.slice(0, 8)})` : "없음"}
      </p>

      <form onSubmit={handleCreate} style={{ display: "grid", gap: 10 }}>
        <input value={tenantId} onChange={(event) => setTenantId(event.target.value)} placeholder="Tenant ID(UUID)" required style={inputStyle} />
        <input value={name} onChange={(event) => setName(event.target.value)} placeholder="VPC Name" required style={inputStyle} />
        <input value={cidrBlock} onChange={(event) => setCidrBlock(event.target.value)} placeholder="CIDR Block" required style={inputStyle} />
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <button type="submit" style={buttonStyle}>
            VPC 생성
          </button>
          <button type="button" style={secondaryButtonStyle} onClick={() => void refresh()}>
            VPC 조회
          </button>
        </div>
      </form>

      <ul style={{ marginTop: 14, paddingLeft: 16 }}>
        {vpcs.map((vpc) => (
          <li key={vpc.id} style={{ marginBottom: 12 }}>
            <strong>{vpc.name}</strong> / {vpc.cidrBlock}
            <div style={subTextStyle}>defaultRouteTable={vpc.defaultRouteTableId.slice(0, 8)}</div>
            <div style={{ marginTop: 6 }}>
              <button type="button" style={dangerButtonStyle} onClick={() => void handleDelete(vpc.id)}>
                Delete
              </button>
            </div>
          </li>
        ))}
      </ul>

      {errorMessage ? <p style={errorStyle}>{errorMessage}</p> : null}
    </section>
  );
}
