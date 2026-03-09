"use client";

import { FormEvent, useEffect, useState } from "react";
import {
  ElasticIpResponse,
  LoadBalancerResponse,
  NatGatewayResponse,
  NetworkElasticIpAttachmentType,
  ProjectResponse,
  attachElasticIp,
  createElasticIp,
  deleteElasticIp,
  detachElasticIp,
  getAuthSession,
  listElasticIps,
  listLoadBalancers,
  listNatGateways
} from "@/shared/lib/api/client";
import { buttonStyle, dangerButtonStyle, errorStyle, inputStyle, panelStyle, secondaryButtonStyle, subTextStyle } from "@/features/network/shared/panelStyles";

interface ElasticIpPanelProps {
  selectedProject: ProjectResponse | null;
}

const attachmentTypes: NetworkElasticIpAttachmentType[] = ["LOAD_BALANCER", "NAT_GATEWAY"];

export function ElasticIpPanel({ selectedProject }: ElasticIpPanelProps) {
  const session = getAuthSession();
  const [tenantId, setTenantId] = useState(session.tenantId);
  const [name, setName] = useState("edge-eip");
  const [elasticIps, setElasticIps] = useState<ElasticIpResponse[]>([]);
  const [loadBalancers, setLoadBalancers] = useState<LoadBalancerResponse[]>([]);
  const [natGateways, setNatGateways] = useState<NatGatewayResponse[]>([]);
  const [selectedElasticIpId, setSelectedElasticIpId] = useState("");
  const [targetType, setTargetType] = useState<NetworkElasticIpAttachmentType>("LOAD_BALANCER");
  const [targetId, setTargetId] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  const refresh = async () => {
    if (!selectedProject) {
      setElasticIps([]);
      setLoadBalancers([]);
      setNatGateways([]);
      setSelectedElasticIpId("");
      setTargetId("");
      return;
    }
    const [latestElasticIps, latestLoadBalancers, latestNatGateways] = await Promise.all([
      listElasticIps(tenantId.trim(), selectedProject.id),
      listLoadBalancers(tenantId.trim(), selectedProject.id),
      listNatGateways(tenantId.trim(), selectedProject.id)
    ]);
    setElasticIps(latestElasticIps);
    setLoadBalancers(latestLoadBalancers);
    setNatGateways(latestNatGateways);
    setSelectedElasticIpId((current) => (current && latestElasticIps.some((item) => item.id === current) ? current : latestElasticIps[0]?.id ?? ""));
    const candidates = targetType === "LOAD_BALANCER" ? latestLoadBalancers : latestNatGateways;
    setTargetId((current) => (current && candidates.some((item) => item.id === current) ? current : candidates[0]?.id ?? ""));
  };

  useEffect(() => {
    const run = async () => {
      try {
        if (!selectedProject) {
          setElasticIps([]);
          setLoadBalancers([]);
          setNatGateways([]);
          setSelectedElasticIpId("");
          setTargetId("");
          return;
        }
        const [latestElasticIps, latestLoadBalancers, latestNatGateways] = await Promise.all([
          listElasticIps(tenantId.trim(), selectedProject.id),
          listLoadBalancers(tenantId.trim(), selectedProject.id),
          listNatGateways(tenantId.trim(), selectedProject.id)
        ]);
        setElasticIps(latestElasticIps);
        setLoadBalancers(latestLoadBalancers);
        setNatGateways(latestNatGateways);
        setSelectedElasticIpId((current) => (current && latestElasticIps.some((item) => item.id === current) ? current : latestElasticIps[0]?.id ?? ""));
        const candidates = targetType === "LOAD_BALANCER" ? latestLoadBalancers : latestNatGateways;
        setTargetId((current) => (current && candidates.some((item) => item.id === current) ? current : candidates[0]?.id ?? ""));
      } catch (error) {
        setErrorMessage(error instanceof Error ? error.message : "Elastic IP 조회에 실패했습니다.");
      }
    };
    void run();
  }, [selectedProject, tenantId, targetType]);

  const handleCreate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!selectedProject) {
      setErrorMessage("프로젝트를 먼저 선택하세요.");
      return;
    }
    setErrorMessage("");
    try {
      await createElasticIp(tenantId.trim(), selectedProject.id, name.trim());
      setName("nat-eip");
      await refresh();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Elastic IP 생성에 실패했습니다.");
    }
  };

  const handleAttach = async () => {
    if (!selectedElasticIpId || !targetId) {
      setErrorMessage("Elastic IP와 연결 대상을 먼저 선택하세요.");
      return;
    }
    setErrorMessage("");
    try {
      await attachElasticIp(selectedElasticIpId, targetType, targetId);
      await refresh();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Elastic IP 연결에 실패했습니다.");
    }
  };

  const candidates = targetType === "LOAD_BALANCER" ? loadBalancers : natGateways;

  return (
    <section style={panelStyle}>
      <h2 style={{ marginTop: 0 }}>Elastic IP 운영</h2>
      <form onSubmit={handleCreate} style={{ display: "grid", gap: 10 }}>
        <input value={tenantId} onChange={(event) => setTenantId(event.target.value)} placeholder="Tenant ID(UUID)" required style={inputStyle} />
        <input value={name} onChange={(event) => setName(event.target.value)} placeholder="Elastic IP Name" required style={inputStyle} />
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <button type="submit" style={buttonStyle}>
            Elastic IP 생성
          </button>
          <button type="button" style={secondaryButtonStyle} onClick={() => void refresh()}>
            Elastic IP 조회
          </button>
        </div>
      </form>

      <div style={{ display: "grid", gap: 8, marginTop: 12 }}>
        <select value={selectedElasticIpId} onChange={(event) => setSelectedElasticIpId(event.target.value)} style={inputStyle}>
          <option value="">작업할 Elastic IP 선택</option>
          {elasticIps.map((elasticIp) => (
            <option key={elasticIp.id} value={elasticIp.id}>
              {elasticIp.name}
            </option>
          ))}
        </select>
        <select value={targetType} onChange={(event) => setTargetType(event.target.value as NetworkElasticIpAttachmentType)} style={inputStyle}>
          {attachmentTypes.map((item) => (
            <option key={item} value={item}>
              {item}
            </option>
          ))}
        </select>
        <select value={targetId} onChange={(event) => setTargetId(event.target.value)} style={inputStyle}>
          <option value="">연결 대상 선택</option>
          {candidates.map((item) => (
            <option key={item.id} value={item.id}>
              {item.name}
            </option>
          ))}
        </select>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <button type="button" style={secondaryButtonStyle} onClick={() => void handleAttach()}>
            연결
          </button>
          <button type="button" style={buttonStyle} onClick={() => void (selectedElasticIpId ? detachElasticIp(selectedElasticIpId).then(refresh).catch((error: unknown) => {
            setErrorMessage(error instanceof Error ? error.message : "Elastic IP 연결 해제에 실패했습니다.");
          }) : Promise.resolve())}>
            연결 해제
          </button>
        </div>
      </div>

      <ul style={{ marginTop: 14, paddingLeft: 16 }}>
        {elasticIps.map((elasticIp) => (
          <li key={elasticIp.id} style={{ marginBottom: 12 }}>
            <strong>{elasticIp.name}</strong> / {elasticIp.publicIp}
            <div style={subTextStyle}>
              {elasticIp.allocationStatus}
              {elasticIp.attachment ? ` · ${elasticIp.attachment.targetType}:${elasticIp.attachment.targetId.slice(0, 8)}` : ""}
            </div>
            <div style={{ marginTop: 6 }}>
              <button type="button" style={dangerButtonStyle} onClick={() => void deleteElasticIp(elasticIp.id).then(refresh).catch((error: unknown) => {
                setErrorMessage(error instanceof Error ? error.message : "Elastic IP 삭제에 실패했습니다.");
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
