"use client";

import { FormEvent, useEffect, useState } from "react";
import {
  NatGatewayResponse,
  ProjectResponse,
  SubnetResponse,
  VpcResponse,
  createNatGateway,
  deleteNatGateway,
  getAuthSession,
  listNatGateways,
  listSubnets,
  listVpcs
} from "@/shared/lib/api/client";
import { buttonStyle, dangerButtonStyle, errorStyle, inputStyle, panelStyle, secondaryButtonStyle, subTextStyle } from "@/features/network/shared/panelStyles";

interface NatGatewayPanelProps {
  selectedProject: ProjectResponse | null;
}

export function NatGatewayPanel({ selectedProject }: NatGatewayPanelProps) {
  const session = getAuthSession();
  const [tenantId, setTenantId] = useState(session.tenantId);
  const [vpcs, setVpcs] = useState<VpcResponse[]>([]);
  const [vpcId, setVpcId] = useState("");
  const [subnets, setSubnets] = useState<SubnetResponse[]>([]);
  const [subnetId, setSubnetId] = useState("");
  const [name, setName] = useState("nat-a");
  const [natGateways, setNatGateways] = useState<NatGatewayResponse[]>([]);
  const [errorMessage, setErrorMessage] = useState("");

  const refresh = async () => {
    if (!selectedProject) {
      setVpcs([]);
      setSubnets([]);
      setNatGateways([]);
      setVpcId("");
      setSubnetId("");
      return;
    }
    const latestVpcs = await listVpcs(tenantId.trim(), selectedProject.id);
    setVpcs(latestVpcs);
    const nextVpcId = vpcId && latestVpcs.some((item) => item.id === vpcId) ? vpcId : latestVpcs[0]?.id ?? "";
    setVpcId(nextVpcId);
    const [latestSubnets, latestNatGateways] = await Promise.all([
      nextVpcId ? listSubnets(tenantId.trim(), selectedProject.id, nextVpcId) : Promise.resolve([]),
      listNatGateways(tenantId.trim(), selectedProject.id)
    ]);
    setSubnets(latestSubnets);
    setSubnetId((current) => (current && latestSubnets.some((item) => item.id === current) ? current : latestSubnets[0]?.id ?? ""));
    setNatGateways(latestNatGateways.filter((item) => !nextVpcId || item.vpcId === nextVpcId));
  };

  useEffect(() => {
    const run = async () => {
      try {
        if (!selectedProject) {
          setVpcs([]);
          setSubnets([]);
          setNatGateways([]);
          setVpcId("");
          setSubnetId("");
          return;
        }
        const latestVpcs = await listVpcs(tenantId.trim(), selectedProject.id);
        setVpcs(latestVpcs);
        const nextVpcId = vpcId && latestVpcs.some((item) => item.id === vpcId) ? vpcId : latestVpcs[0]?.id ?? "";
        setVpcId(nextVpcId);
        const [latestSubnets, latestNatGateways] = await Promise.all([
          nextVpcId ? listSubnets(tenantId.trim(), selectedProject.id, nextVpcId) : Promise.resolve([]),
          listNatGateways(tenantId.trim(), selectedProject.id)
        ]);
        setSubnets(latestSubnets);
        setSubnetId((current) => (current && latestSubnets.some((item) => item.id === current) ? current : latestSubnets[0]?.id ?? ""));
        setNatGateways(latestNatGateways.filter((item) => !nextVpcId || item.vpcId === nextVpcId));
      } catch (error) {
        setErrorMessage(error instanceof Error ? error.message : "NAT Gateway 조회에 실패했습니다.");
      }
    };
    void run();
  }, [selectedProject, tenantId, vpcId]);

  const handleCreate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!selectedProject || !vpcId || !subnetId) {
      setErrorMessage("프로젝트, VPC, Subnet을 먼저 선택하세요.");
      return;
    }
    setErrorMessage("");
    try {
      await createNatGateway(tenantId.trim(), selectedProject.id, vpcId, subnetId, name.trim());
      setName("nat-b");
      await refresh();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "NAT Gateway 생성에 실패했습니다.");
    }
  };

  return (
    <section style={panelStyle}>
      <h2 style={{ marginTop: 0 }}>NAT Gateway 운영</h2>
      <form onSubmit={handleCreate} style={{ display: "grid", gap: 10 }}>
        <input value={tenantId} onChange={(event) => setTenantId(event.target.value)} placeholder="Tenant ID(UUID)" required style={inputStyle} />
        <select value={vpcId} onChange={(event) => setVpcId(event.target.value)} style={inputStyle}>
          <option value="">VPC 선택</option>
          {vpcs.map((vpc) => (
            <option key={vpc.id} value={vpc.id}>
              {vpc.name}
            </option>
          ))}
        </select>
        <select value={subnetId} onChange={(event) => setSubnetId(event.target.value)} style={inputStyle}>
          <option value="">Subnet 선택</option>
          {subnets.map((subnet) => (
            <option key={subnet.id} value={subnet.id}>
              {subnet.name}
            </option>
          ))}
        </select>
        <input value={name} onChange={(event) => setName(event.target.value)} placeholder="NAT Gateway Name" required style={inputStyle} />
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <button type="submit" style={buttonStyle}>
            NAT 생성
          </button>
          <button type="button" style={secondaryButtonStyle} onClick={() => void refresh()}>
            NAT 조회
          </button>
        </div>
      </form>

      <ul style={{ marginTop: 14, paddingLeft: 16 }}>
        {natGateways.map((natGateway) => (
          <li key={natGateway.id} style={{ marginBottom: 12 }}>
            <strong>{natGateway.name}</strong>
            <div style={subTextStyle}>
              subnet={natGateway.subnetId.slice(0, 8)} · vpc={natGateway.vpcId.slice(0, 8)}
            </div>
            <div style={{ marginTop: 6 }}>
              <button type="button" style={dangerButtonStyle} onClick={() => void deleteNatGateway(natGateway.id).then(refresh).catch((error: unknown) => {
                setErrorMessage(error instanceof Error ? error.message : "NAT Gateway 삭제에 실패했습니다.");
              })}>
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
