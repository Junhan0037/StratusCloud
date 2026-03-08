"use client";

import { FormEvent, useEffect, useState } from "react";
import {
  ProjectResponse,
  SubnetResponse,
  VpcResponse,
  createSubnet,
  deleteSubnet,
  getAuthSession,
  listSubnets,
  listVpcs
} from "@/shared/lib/api/client";
import { buttonStyle, dangerButtonStyle, errorStyle, inputStyle, panelStyle, secondaryButtonStyle, subTextStyle } from "@/features/network/shared/panelStyles";

interface SubnetPanelProps {
  selectedProject: ProjectResponse | null;
}

export function SubnetPanel({ selectedProject }: SubnetPanelProps) {
  const session = getAuthSession();
  const [tenantId, setTenantId] = useState(session.tenantId);
  const [vpcs, setVpcs] = useState<VpcResponse[]>([]);
  const [vpcId, setVpcId] = useState("");
  const [name, setName] = useState("private-a");
  const [cidrBlock, setCidrBlock] = useState("10.0.1.0/24");
  const [availabilityZone, setAvailabilityZone] = useState("ap-northeast-2a");
  const [subnets, setSubnets] = useState<SubnetResponse[]>([]);
  const [errorMessage, setErrorMessage] = useState("");

  const refreshVpcs = async () => {
    if (!selectedProject) {
      setVpcs([]);
      setVpcId("");
      return;
    }
    const latest = await listVpcs(tenantId.trim(), selectedProject.id);
    setVpcs(latest);
    setVpcId((current) => (current && latest.some((item) => item.id === current) ? current : latest[0]?.id ?? ""));
  };

  const refreshSubnets = async () => {
    if (!selectedProject) {
      setSubnets([]);
      return;
    }
    const latest = await listSubnets(tenantId.trim(), selectedProject.id, vpcId || undefined);
    setSubnets(latest);
  };

  useEffect(() => {
    const run = async () => {
      try {
        if (!selectedProject) {
          setVpcs([]);
          setVpcId("");
          return;
        }
        const latest = await listVpcs(tenantId.trim(), selectedProject.id);
        setVpcs(latest);
        setVpcId((current) => (current && latest.some((item) => item.id === current) ? current : latest[0]?.id ?? ""));
      } catch (error) {
        setErrorMessage(error instanceof Error ? error.message : "VPC 조회에 실패했습니다.");
      }
    };
    void run();
  }, [selectedProject, tenantId]);

  useEffect(() => {
    const run = async () => {
      try {
        if (!selectedProject) {
          setSubnets([]);
          return;
        }
        const latest = await listSubnets(tenantId.trim(), selectedProject.id, vpcId || undefined);
        setSubnets(latest);
      } catch (error) {
        setErrorMessage(error instanceof Error ? error.message : "Subnet 조회에 실패했습니다.");
      }
    };
    void run();
  }, [selectedProject, tenantId, vpcId]);

  const handleCreate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!selectedProject || !vpcId) {
      setErrorMessage("프로젝트와 VPC를 먼저 선택하세요.");
      return;
    }
    setErrorMessage("");
    try {
      await createSubnet(tenantId.trim(), selectedProject.id, vpcId, name.trim(), cidrBlock.trim(), availabilityZone.trim());
      setName("private-b");
      await refreshSubnets();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Subnet 생성에 실패했습니다.");
    }
  };

  const handleDelete = async (subnetId: string) => {
    setErrorMessage("");
    try {
      await deleteSubnet(subnetId);
      await refreshSubnets();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Subnet 삭제에 실패했습니다.");
    }
  };

  return (
    <section style={panelStyle}>
      <h2 style={{ marginTop: 0 }}>Subnet 운영</h2>
      <form onSubmit={handleCreate} style={{ display: "grid", gap: 10 }}>
        <input value={tenantId} onChange={(event) => setTenantId(event.target.value)} placeholder="Tenant ID(UUID)" required style={inputStyle} />
        <select value={vpcId} onChange={(event) => setVpcId(event.target.value)} style={inputStyle}>
          <option value="">VPC 선택</option>
          {vpcs.map((vpc) => (
            <option key={vpc.id} value={vpc.id}>
              {vpc.name} ({vpc.cidrBlock})
            </option>
          ))}
        </select>
        <input value={name} onChange={(event) => setName(event.target.value)} placeholder="Subnet Name" required style={inputStyle} />
        <input value={cidrBlock} onChange={(event) => setCidrBlock(event.target.value)} placeholder="CIDR Block" required style={inputStyle} />
        <input value={availabilityZone} onChange={(event) => setAvailabilityZone(event.target.value)} placeholder="Availability Zone" required style={inputStyle} />
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <button type="submit" style={buttonStyle}>
            Subnet 생성
          </button>
          <button type="button" style={secondaryButtonStyle} onClick={() => void refreshSubnets()}>
            Subnet 조회
          </button>
        </div>
      </form>

      <ul style={{ marginTop: 14, paddingLeft: 16 }}>
        {subnets.map((subnet) => (
          <li key={subnet.id} style={{ marginBottom: 12 }}>
            <strong>{subnet.name}</strong> / {subnet.cidrBlock}
            <div style={subTextStyle}>
              az={subnet.availabilityZone} · routeTable={subnet.routeTableId?.slice(0, 8) ?? "none"}
            </div>
            <div style={{ marginTop: 6 }}>
              <button type="button" style={dangerButtonStyle} onClick={() => void handleDelete(subnet.id)}>
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
