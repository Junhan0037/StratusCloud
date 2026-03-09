"use client";

import { FormEvent, useEffect, useState } from "react";
import {
  NatGatewayResponse,
  NetworkRouteTargetType,
  ProjectResponse,
  RouteTableResponse,
  SubnetResponse,
  VpcResponse,
  associateSubnet,
  createRoute,
  createRouteTable,
  deleteRoute,
  deleteRouteTable,
  getAuthSession,
  listNatGateways,
  listRouteTables,
  listSubnets,
  listVpcs
} from "@/shared/lib/api/client";
import { buttonStyle, dangerButtonStyle, errorStyle, inlineButtonStyle, inputStyle, panelStyle, secondaryButtonStyle, subTextStyle } from "@/features/network/shared/panelStyles";

interface RouteTablePanelProps {
  selectedProject: ProjectResponse | null;
}

const routeTargets: NetworkRouteTargetType[] = ["INTERNET_GATEWAY", "NAT_GATEWAY"];

export function RouteTablePanel({ selectedProject }: RouteTablePanelProps) {
  const session = getAuthSession();
  const [tenantId, setTenantId] = useState(session.tenantId);
  const [vpcs, setVpcs] = useState<VpcResponse[]>([]);
  const [vpcId, setVpcId] = useState("");
  const [subnets, setSubnets] = useState<SubnetResponse[]>([]);
  const [natGateways, setNatGateways] = useState<NatGatewayResponse[]>([]);
  const [routeTables, setRouteTables] = useState<RouteTableResponse[]>([]);
  const [name, setName] = useState("edge-rt");
  const [routeTableId, setRouteTableId] = useState("");
  const [destinationCidr, setDestinationCidr] = useState("0.0.0.0/0");
  const [targetType, setTargetType] = useState<NetworkRouteTargetType>("INTERNET_GATEWAY");
  const [targetResourceId, setTargetResourceId] = useState("");
  const [associateSubnetId, setAssociateSubnetId] = useState("");
  const [errorMessage, setErrorMessage] = useState("");

  const refreshContext = async () => {
    if (!selectedProject) {
      setVpcs([]);
      setVpcId("");
      setSubnets([]);
      setNatGateways([]);
      setRouteTables([]);
      return;
    }
    const latestVpcs = await listVpcs(tenantId.trim(), selectedProject.id);
    setVpcs(latestVpcs);
    const nextVpcId = vpcId && latestVpcs.some((item) => item.id === vpcId) ? vpcId : latestVpcs[0]?.id ?? "";
    setVpcId(nextVpcId);
    if (!nextVpcId) {
      setSubnets([]);
      setRouteTables([]);
      return;
    }
    const [latestSubnets, latestRouteTables, latestNatGateways] = await Promise.all([
      listSubnets(tenantId.trim(), selectedProject.id, nextVpcId),
      listRouteTables(nextVpcId),
      listNatGateways(tenantId.trim(), selectedProject.id)
    ]);
    const filteredNatGateways = latestNatGateways.filter((item) => item.vpcId === nextVpcId);
    setSubnets(latestSubnets);
    setNatGateways(filteredNatGateways);
    setAssociateSubnetId((current) => (current && latestSubnets.some((item) => item.id === current) ? current : latestSubnets[0]?.id ?? ""));
    setRouteTables(latestRouteTables);
    setRouteTableId((current) => (current && latestRouteTables.some((item) => item.id === current) ? current : latestRouteTables[0]?.id ?? ""));
    setTargetResourceId((current) => (current && filteredNatGateways.some((item) => item.id === current) ? current : filteredNatGateways[0]?.id ?? ""));
  };

  useEffect(() => {
    const run = async () => {
      try {
        if (!selectedProject) {
          setVpcs([]);
          setVpcId("");
          setSubnets([]);
          setNatGateways([]);
          setRouteTables([]);
          return;
        }
        const latestVpcs = await listVpcs(tenantId.trim(), selectedProject.id);
        setVpcs(latestVpcs);
        const nextVpcId = vpcId && latestVpcs.some((item) => item.id === vpcId) ? vpcId : latestVpcs[0]?.id ?? "";
        setVpcId(nextVpcId);
        if (!nextVpcId) {
          setSubnets([]);
          setRouteTables([]);
          return;
        }
        const [latestSubnets, latestRouteTables, latestNatGateways] = await Promise.all([
          listSubnets(tenantId.trim(), selectedProject.id, nextVpcId),
          listRouteTables(nextVpcId),
          listNatGateways(tenantId.trim(), selectedProject.id)
        ]);
        const filteredNatGateways = latestNatGateways.filter((item) => item.vpcId === nextVpcId);
        setSubnets(latestSubnets);
        setNatGateways(filteredNatGateways);
        setAssociateSubnetId((current) => (current && latestSubnets.some((item) => item.id === current) ? current : latestSubnets[0]?.id ?? ""));
        setRouteTables(latestRouteTables);
        setRouteTableId((current) => (current && latestRouteTables.some((item) => item.id === current) ? current : latestRouteTables[0]?.id ?? ""));
        setTargetResourceId((current) => (current && filteredNatGateways.some((item) => item.id === current) ? current : filteredNatGateways[0]?.id ?? ""));
      } catch (error) {
        setErrorMessage(error instanceof Error ? error.message : "Route Table 조회에 실패했습니다.");
      }
    };
    void run();
  }, [selectedProject, tenantId, vpcId]);

  const handleCreateRouteTable = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!selectedProject || !vpcId) {
      setErrorMessage("프로젝트와 VPC를 먼저 선택하세요.");
      return;
    }
    setErrorMessage("");
    try {
      await createRouteTable(tenantId.trim(), selectedProject.id, vpcId, name.trim());
      await refreshContext();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Route Table 생성에 실패했습니다.");
    }
  };

  const handleCreateRoute = async () => {
    if (!routeTableId) {
      setErrorMessage("Route Table을 먼저 선택하세요.");
      return;
    }
    if (targetType === "NAT_GATEWAY" && !targetResourceId) {
      setErrorMessage("NAT Gateway를 먼저 선택하세요.");
      return;
    }
    setErrorMessage("");
    try {
      await createRoute(routeTableId, destinationCidr.trim(), targetType, targetType === "NAT_GATEWAY" ? targetResourceId : undefined);
      await refreshContext();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Route 생성에 실패했습니다.");
    }
  };

  const handleAssociate = async () => {
    if (!routeTableId || !associateSubnetId) {
      setErrorMessage("Route Table과 Subnet을 먼저 선택하세요.");
      return;
    }
    setErrorMessage("");
    try {
      await associateSubnet(routeTableId, associateSubnetId);
      await refreshContext();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Subnet 연결에 실패했습니다.");
    }
  };

  return (
    <section style={panelStyle}>
      <h2 style={{ marginTop: 0 }}>Route Table 운영</h2>
      <form onSubmit={handleCreateRouteTable} style={{ display: "grid", gap: 10 }}>
        <input value={tenantId} onChange={(event) => setTenantId(event.target.value)} placeholder="Tenant ID(UUID)" required style={inputStyle} />
        <select value={vpcId} onChange={(event) => setVpcId(event.target.value)} style={inputStyle}>
          <option value="">VPC 선택</option>
          {vpcs.map((vpc) => (
            <option key={vpc.id} value={vpc.id}>
              {vpc.name}
            </option>
          ))}
        </select>
        <input value={name} onChange={(event) => setName(event.target.value)} placeholder="Route Table Name" required style={inputStyle} />
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <button type="submit" style={buttonStyle}>
            Route Table 생성
          </button>
          <button type="button" style={secondaryButtonStyle} onClick={() => void refreshContext()}>
            Route Table 조회
          </button>
        </div>
      </form>

      <div style={{ display: "grid", gap: 8, marginTop: 12 }}>
        <select value={routeTableId} onChange={(event) => setRouteTableId(event.target.value)} style={inputStyle}>
          <option value="">작업할 Route Table 선택</option>
          {routeTables.map((item) => (
            <option key={item.id} value={item.id}>
              {item.name}
            </option>
          ))}
        </select>
        <input value={destinationCidr} onChange={(event) => setDestinationCidr(event.target.value)} placeholder="Destination CIDR" style={inputStyle} />
        <select value={targetType} onChange={(event) => setTargetType(event.target.value as NetworkRouteTargetType)} style={inputStyle}>
          {routeTargets.map((item) => (
            <option key={item} value={item}>
              {item}
            </option>
          ))}
        </select>
        {targetType === "NAT_GATEWAY" ? (
          <select value={targetResourceId} onChange={(event) => setTargetResourceId(event.target.value)} style={inputStyle}>
            <option value="">NAT Gateway 선택</option>
            {natGateways.map((item) => (
              <option key={item.id} value={item.id}>
                {item.name}
              </option>
            ))}
          </select>
        ) : null}
        <button type="button" style={inlineButtonStyle} onClick={() => void handleCreateRoute()}>
          Route 추가
        </button>
        <select value={associateSubnetId} onChange={(event) => setAssociateSubnetId(event.target.value)} style={inputStyle}>
          <option value="">연결할 Subnet 선택</option>
          {subnets.map((item) => (
            <option key={item.id} value={item.id}>
              {item.name}
            </option>
          ))}
        </select>
        <button type="button" style={secondaryButtonStyle} onClick={() => void handleAssociate()}>
          Subnet 연결
        </button>
      </div>

      <ul style={{ marginTop: 14, paddingLeft: 16 }}>
        {routeTables.map((table) => (
          <li key={table.id} style={{ marginBottom: 12 }}>
            <strong>{table.name}</strong> / {table.isDefault ? "default" : "custom"}
            <div style={subTextStyle}>routes={table.routes.length} · associations={table.associations.length}</div>
            <ul style={{ marginTop: 6, paddingLeft: 16 }}>
              {table.routes.map((route) => (
                <li key={route.id} style={{ marginBottom: 6 }}>
                  {route.destinationCidr} → {route.targetType}{route.targetResourceId ? ` (${route.targetResourceId.slice(0, 8)})` : ""}
                  {route.targetType !== "LOCAL" ? (
                    <button
                      type="button"
                      style={{ ...dangerButtonStyle, marginLeft: 8 }}
                      onClick={() => void deleteRoute(route.id).then(refreshContext).catch((error: unknown) => {
                        setErrorMessage(error instanceof Error ? error.message : "Route 삭제에 실패했습니다.");
                      })}
                    >
                      Delete Route
                    </button>
                  ) : null}
                </li>
              ))}
            </ul>
            {!table.isDefault ? (
              <button
                type="button"
                style={dangerButtonStyle}
                onClick={() => void deleteRouteTable(table.id).then(refreshContext).catch((error: unknown) => {
                  setErrorMessage(error instanceof Error ? error.message : "Route Table 삭제에 실패했습니다.");
                })}
              >
                Delete Table
              </button>
            ) : null}
          </li>
        ))}
      </ul>

      {errorMessage ? <p style={errorStyle}>{errorMessage}</p> : null}
    </section>
  );
}
