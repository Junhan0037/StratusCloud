"use client";

import { FormEvent, useEffect, useState } from "react";
import {
  DnsRecordResponse,
  ElasticIpResponse,
  LoadBalancerResponse,
  NetworkDnsTargetType,
  ProjectResponse,
  createDnsRecord,
  deleteDnsRecord,
  getAuthSession,
  listDnsRecords,
  listElasticIps,
  listLoadBalancers
} from "@/shared/lib/api/client";
import { buttonStyle, dangerButtonStyle, errorStyle, inputStyle, panelStyle, secondaryButtonStyle, subTextStyle } from "@/features/network/shared/panelStyles";

interface DnsRecordPanelProps {
  selectedProject: ProjectResponse | null;
}

const dnsTargetTypes: NetworkDnsTargetType[] = ["LOAD_BALANCER", "ELASTIC_IP"];

export function DnsRecordPanel({ selectedProject }: DnsRecordPanelProps) {
  const session = getAuthSession();
  const [tenantId, setTenantId] = useState(session.tenantId);
  const [name, setName] = useState("api.internal");
  const [targetType, setTargetType] = useState<NetworkDnsTargetType>("LOAD_BALANCER");
  const [targetId, setTargetId] = useState("");
  const [dnsRecords, setDnsRecords] = useState<DnsRecordResponse[]>([]);
  const [loadBalancers, setLoadBalancers] = useState<LoadBalancerResponse[]>([]);
  const [elasticIps, setElasticIps] = useState<ElasticIpResponse[]>([]);
  const [errorMessage, setErrorMessage] = useState("");

  const refresh = async () => {
    if (!selectedProject) {
      setDnsRecords([]);
      setLoadBalancers([]);
      setElasticIps([]);
      setTargetId("");
      return;
    }
    const [latestDnsRecords, latestLoadBalancers, latestElasticIps] = await Promise.all([
      listDnsRecords(tenantId.trim(), selectedProject.id),
      listLoadBalancers(tenantId.trim(), selectedProject.id),
      listElasticIps(tenantId.trim(), selectedProject.id)
    ]);
    setDnsRecords(latestDnsRecords);
    setLoadBalancers(latestLoadBalancers);
    setElasticIps(latestElasticIps);
    const candidates = targetType === "LOAD_BALANCER" ? latestLoadBalancers : latestElasticIps;
    setTargetId((current) => (current && candidates.some((item) => item.id === current) ? current : candidates[0]?.id ?? ""));
  };

  useEffect(() => {
    const run = async () => {
      try {
        if (!selectedProject) {
          setDnsRecords([]);
          setLoadBalancers([]);
          setElasticIps([]);
          setTargetId("");
          return;
        }
        const [latestDnsRecords, latestLoadBalancers, latestElasticIps] = await Promise.all([
          listDnsRecords(tenantId.trim(), selectedProject.id),
          listLoadBalancers(tenantId.trim(), selectedProject.id),
          listElasticIps(tenantId.trim(), selectedProject.id)
        ]);
        setDnsRecords(latestDnsRecords);
        setLoadBalancers(latestLoadBalancers);
        setElasticIps(latestElasticIps);
        const candidates = targetType === "LOAD_BALANCER" ? latestLoadBalancers : latestElasticIps;
        setTargetId((current) => (current && candidates.some((item) => item.id === current) ? current : candidates[0]?.id ?? ""));
      } catch (error) {
        setErrorMessage(error instanceof Error ? error.message : "DNS Record 조회에 실패했습니다.");
      }
    };
    void run();
  }, [selectedProject, tenantId, targetType]);

  const handleCreate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!selectedProject || !targetId) {
      setErrorMessage("프로젝트와 DNS 대상을 먼저 선택하세요.");
      return;
    }
    setErrorMessage("");
    try {
      await createDnsRecord(tenantId.trim(), selectedProject.id, name.trim(), targetType, targetId);
      setName("nat.internal");
      await refresh();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "DNS Record 생성에 실패했습니다.");
    }
  };

  const candidates = targetType === "LOAD_BALANCER" ? loadBalancers : elasticIps;

  return (
    <section style={panelStyle}>
      <h2 style={{ marginTop: 0 }}>DNS Record 운영</h2>
      <form onSubmit={handleCreate} style={{ display: "grid", gap: 10 }}>
        <input value={tenantId} onChange={(event) => setTenantId(event.target.value)} placeholder="Tenant ID(UUID)" required style={inputStyle} />
        <input value={name} onChange={(event) => setName(event.target.value)} placeholder="DNS Name" required style={inputStyle} />
        <select value={targetType} onChange={(event) => setTargetType(event.target.value as NetworkDnsTargetType)} style={inputStyle}>
          {dnsTargetTypes.map((item) => (
            <option key={item} value={item}>
              {item}
            </option>
          ))}
        </select>
        <select value={targetId} onChange={(event) => setTargetId(event.target.value)} style={inputStyle}>
          <option value="">대상 선택</option>
          {candidates.map((item) => (
            <option key={item.id} value={item.id}>
              {item.name}
            </option>
          ))}
        </select>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <button type="submit" style={buttonStyle}>
            DNS 생성
          </button>
          <button type="button" style={secondaryButtonStyle} onClick={() => void refresh()}>
            DNS 조회
          </button>
        </div>
      </form>

      <ul style={{ marginTop: 14, paddingLeft: 16 }}>
        {dnsRecords.map((dnsRecord) => (
          <li key={dnsRecord.id} style={{ marginBottom: 12 }}>
            <strong>{dnsRecord.name}</strong> / {dnsRecord.recordType}
            <div style={subTextStyle}>
              {dnsRecord.targetType}:{dnsRecord.targetId.slice(0, 8)}
            </div>
            <div style={{ marginTop: 6 }}>
              <button type="button" style={dangerButtonStyle} onClick={() => void deleteDnsRecord(dnsRecord.id).then(refresh).catch((error: unknown) => {
                setErrorMessage(error instanceof Error ? error.message : "DNS Record 삭제에 실패했습니다.");
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
