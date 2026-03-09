"use client";

import { FormEvent, useEffect, useState } from "react";
import {
  LoadBalancerResponse,
  NetworkLoadBalancerProtocol,
  NetworkLoadBalancerScheme,
  NetworkLoadBalancerType,
  ProjectResponse,
  SubnetResponse,
  VpcResponse,
  createLoadBalancer,
  createLoadBalancerListener,
  createLoadBalancerRule,
  deleteLoadBalancer,
  getAuthSession,
  listLoadBalancers,
  listSubnets,
  listVpcs
} from "@/shared/lib/api/client";
import { buttonStyle, dangerButtonStyle, errorStyle, inputStyle, panelStyle, secondaryButtonStyle, subTextStyle } from "@/features/network/shared/panelStyles";

interface LoadBalancerPanelProps {
  selectedProject: ProjectResponse | null;
}

const loadBalancerTypes: NetworkLoadBalancerType[] = ["L4", "L7"];
const loadBalancerSchemes: NetworkLoadBalancerScheme[] = ["INTERNAL", "INTERNET_FACING"];

export function LoadBalancerPanel({ selectedProject }: LoadBalancerPanelProps) {
  const session = getAuthSession();
  const [tenantId, setTenantId] = useState(session.tenantId);
  const [vpcs, setVpcs] = useState<VpcResponse[]>([]);
  const [vpcId, setVpcId] = useState("");
  const [subnets, setSubnets] = useState<SubnetResponse[]>([]);
  const [loadBalancers, setLoadBalancers] = useState<LoadBalancerResponse[]>([]);
  const [name, setName] = useState("edge-http");
  const [type, setType] = useState<NetworkLoadBalancerType>("L7");
  const [scheme, setScheme] = useState<NetworkLoadBalancerScheme>("INTERNET_FACING");
  const [selectedLoadBalancerId, setSelectedLoadBalancerId] = useState("");
  const [listenerProtocol, setListenerProtocol] = useState<NetworkLoadBalancerProtocol>("HTTP");
  const [listenerPort, setListenerPort] = useState("80");
  const [defaultTargetSubnetId, setDefaultTargetSubnetId] = useState("");
  const [selectedListenerId, setSelectedListenerId] = useState("");
  const [priority, setPriority] = useState("10");
  const [pathPattern, setPathPattern] = useState("/api/*");
  const [ruleTargetSubnetId, setRuleTargetSubnetId] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  const refresh = async () => {
    if (!selectedProject) {
      setVpcs([]);
      setSubnets([]);
      setLoadBalancers([]);
      return;
    }
    const latestVpcs = await listVpcs(tenantId.trim(), selectedProject.id);
    setVpcs(latestVpcs);
    const nextVpcId = vpcId && latestVpcs.some((item) => item.id === vpcId) ? vpcId : latestVpcs[0]?.id ?? "";
    setVpcId(nextVpcId);
    const [latestSubnets, latestLoadBalancers] = await Promise.all([
      nextVpcId ? listSubnets(tenantId.trim(), selectedProject.id, nextVpcId) : Promise.resolve([]),
      listLoadBalancers(tenantId.trim(), selectedProject.id)
    ]);
    const filteredLoadBalancers = latestLoadBalancers.filter((item) => !nextVpcId || item.vpcId === nextVpcId);
    setSubnets(latestSubnets);
    setLoadBalancers(filteredLoadBalancers);
    setDefaultTargetSubnetId((current) => (current && latestSubnets.some((item) => item.id === current) ? current : latestSubnets[0]?.id ?? ""));
    setRuleTargetSubnetId((current) => (current && latestSubnets.some((item) => item.id === current) ? current : latestSubnets[0]?.id ?? ""));
    setSelectedLoadBalancerId((current) => (current && filteredLoadBalancers.some((item) => item.id === current) ? current : filteredLoadBalancers[0]?.id ?? ""));
    const nextLoadBalancer = filteredLoadBalancers.find((item) => item.id === (selectedLoadBalancerId || filteredLoadBalancers[0]?.id));
    setSelectedListenerId((current) => (current && nextLoadBalancer?.listeners.some((item) => item.id === current) ? current : nextLoadBalancer?.listeners[0]?.id ?? ""));
  };

  useEffect(() => {
    const run = async () => {
      try {
        if (!selectedProject) {
          setVpcs([]);
          setSubnets([]);
          setLoadBalancers([]);
          return;
        }
        const latestVpcs = await listVpcs(tenantId.trim(), selectedProject.id);
        setVpcs(latestVpcs);
        const nextVpcId = vpcId && latestVpcs.some((item) => item.id === vpcId) ? vpcId : latestVpcs[0]?.id ?? "";
        setVpcId(nextVpcId);
        const [latestSubnets, latestLoadBalancers] = await Promise.all([
          nextVpcId ? listSubnets(tenantId.trim(), selectedProject.id, nextVpcId) : Promise.resolve([]),
          listLoadBalancers(tenantId.trim(), selectedProject.id)
        ]);
        const filteredLoadBalancers = latestLoadBalancers.filter((item) => !nextVpcId || item.vpcId === nextVpcId);
        setSubnets(latestSubnets);
        setLoadBalancers(filteredLoadBalancers);
        setDefaultTargetSubnetId((current) => (current && latestSubnets.some((item) => item.id === current) ? current : latestSubnets[0]?.id ?? ""));
        setRuleTargetSubnetId((current) => (current && latestSubnets.some((item) => item.id === current) ? current : latestSubnets[0]?.id ?? ""));
        const nextLoadBalancerId = selectedLoadBalancerId && filteredLoadBalancers.some((item) => item.id === selectedLoadBalancerId)
          ? selectedLoadBalancerId
          : filteredLoadBalancers[0]?.id ?? "";
        setSelectedLoadBalancerId(nextLoadBalancerId)
        const nextLoadBalancer = filteredLoadBalancers.find((item) => item.id === nextLoadBalancerId)
        setSelectedListenerId((current) => (current && nextLoadBalancer?.listeners.some((item) => item.id === current) ? current : nextLoadBalancer?.listeners[0]?.id ?? ""));
      } catch (error) {
        setErrorMessage(error instanceof Error ? error.message : "Load Balancer 조회에 실패했습니다.");
      }
    };
    void run();
  }, [selectedProject, tenantId, vpcId, selectedLoadBalancerId]);

  const selectedLoadBalancer = loadBalancers.find((item) => item.id === selectedLoadBalancerId) ?? null;

  const handleCreate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!selectedProject || !vpcId) {
      setErrorMessage("프로젝트와 VPC를 먼저 선택하세요.");
      return;
    }
    setErrorMessage("");
    try {
      await createLoadBalancer(tenantId.trim(), selectedProject.id, vpcId, name.trim(), type, scheme);
      await refresh();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Load Balancer 생성에 실패했습니다.");
    }
  };

  const handleCreateListener = async () => {
    if (!selectedLoadBalancerId || !defaultTargetSubnetId) {
      setErrorMessage("Load Balancer와 기본 대상 Subnet을 먼저 선택하세요.");
      return;
    }
    setErrorMessage("");
    try {
      await createLoadBalancerListener(selectedLoadBalancerId, listenerProtocol, Number(listenerPort), defaultTargetSubnetId);
      await refresh();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Listener 생성에 실패했습니다.");
    }
  };

  const handleCreateRule = async () => {
    if (!selectedListenerId || !ruleTargetSubnetId) {
      setErrorMessage("Listener와 대상 Subnet을 먼저 선택하세요.");
      return;
    }
    setErrorMessage("");
    try {
      await createLoadBalancerRule(selectedListenerId, Number(priority), pathPattern.trim(), ruleTargetSubnetId);
      await refresh();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Rule 생성에 실패했습니다.");
    }
  };

  return (
    <section style={panelStyle}>
      <h2 style={{ marginTop: 0 }}>Load Balancer 운영</h2>
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
        <input value={name} onChange={(event) => setName(event.target.value)} placeholder="Load Balancer Name" required style={inputStyle} />
        <select
          value={type}
          onChange={(event) => {
            const nextType = event.target.value as NetworkLoadBalancerType;
            setType(nextType);
            setListenerProtocol(nextType === "L7" ? "HTTP" : "TCP");
            setListenerPort(nextType === "L7" ? "80" : "443");
          }}
          style={inputStyle}
        >
          {loadBalancerTypes.map((item) => (
            <option key={item} value={item}>
              {item}
            </option>
          ))}
        </select>
        <select value={scheme} onChange={(event) => setScheme(event.target.value as NetworkLoadBalancerScheme)} style={inputStyle}>
          {loadBalancerSchemes.map((item) => (
            <option key={item} value={item}>
              {item}
            </option>
          ))}
        </select>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <button type="submit" style={buttonStyle}>
            LB 생성
          </button>
          <button type="button" style={secondaryButtonStyle} onClick={() => void refresh()}>
            LB 조회
          </button>
        </div>
      </form>

      <div style={{ display: "grid", gap: 8, marginTop: 12 }}>
        <select value={selectedLoadBalancerId} onChange={(event) => setSelectedLoadBalancerId(event.target.value)} style={inputStyle}>
          <option value="">작업할 LB 선택</option>
          {loadBalancers.map((item) => (
            <option key={item.id} value={item.id}>
              {item.name}
            </option>
          ))}
        </select>
        <select value={listenerProtocol} onChange={(event) => setListenerProtocol(event.target.value as NetworkLoadBalancerProtocol)} style={inputStyle}>
          <option value="HTTP">HTTP</option>
          <option value="TCP">TCP</option>
        </select>
        <input value={listenerPort} onChange={(event) => setListenerPort(event.target.value)} placeholder="Listener Port" style={inputStyle} />
        <select value={defaultTargetSubnetId} onChange={(event) => setDefaultTargetSubnetId(event.target.value)} style={inputStyle}>
          <option value="">기본 대상 Subnet 선택</option>
          {subnets.map((subnet) => (
            <option key={subnet.id} value={subnet.id}>
              {subnet.name}
            </option>
          ))}
        </select>
        <button type="button" style={secondaryButtonStyle} onClick={() => void handleCreateListener()}>
          Listener 생성
        </button>

        <select value={selectedListenerId} onChange={(event) => setSelectedListenerId(event.target.value)} style={inputStyle}>
          <option value="">Rule을 추가할 Listener 선택</option>
          {selectedLoadBalancer?.listeners.map((listener) => (
            <option key={listener.id} value={listener.id}>
              {listener.protocol}:{listener.port}
            </option>
          ))}
        </select>
        <input value={priority} onChange={(event) => setPriority(event.target.value)} placeholder="Priority" style={inputStyle} />
        <input value={pathPattern} onChange={(event) => setPathPattern(event.target.value)} placeholder="Path Pattern" style={inputStyle} />
        <select value={ruleTargetSubnetId} onChange={(event) => setRuleTargetSubnetId(event.target.value)} style={inputStyle}>
          <option value="">Rule 대상 Subnet 선택</option>
          {subnets.map((subnet) => (
            <option key={subnet.id} value={subnet.id}>
              {subnet.name}
            </option>
          ))}
        </select>
        <button type="button" style={buttonStyle} onClick={() => void handleCreateRule()}>
          Rule 생성
        </button>
      </div>

      <ul style={{ marginTop: 14, paddingLeft: 16 }}>
        {loadBalancers.map((loadBalancer) => (
          <li key={loadBalancer.id} style={{ marginBottom: 12 }}>
            <strong>{loadBalancer.name}</strong> / {loadBalancer.type} / {loadBalancer.scheme}
            <div style={subTextStyle}>listeners={loadBalancer.listeners.length}</div>
            <ul style={{ marginTop: 6, paddingLeft: 16 }}>
              {loadBalancer.listeners.map((listener) => (
                <li key={listener.id} style={{ marginBottom: 6 }}>
                  {listener.protocol}:{listener.port} → {listener.defaultTargetSubnetId.slice(0, 8)} / rules={listener.rules.length}
                </li>
              ))}
            </ul>
            <button type="button" style={dangerButtonStyle} onClick={() => void deleteLoadBalancer(loadBalancer.id).then(refresh).catch((error: unknown) => {
              setErrorMessage(error instanceof Error ? error.message : "Load Balancer 삭제에 실패했습니다.");
            })}>
              Delete
            </button>
          </li>
        ))}
      </ul>

      {errorMessage ? <p style={errorStyle}>{errorMessage}</p> : null}
    </section>
  );
}
