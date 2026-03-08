"use client";

import { FormEvent, useEffect, useState } from "react";
import {
  NetworkRuleDirection,
  NetworkRuleProtocol,
  ProjectResponse,
  SecurityGroupResponse,
  VpcResponse,
  createSecurityGroup,
  deleteSecurityGroup,
  getAuthSession,
  listSecurityGroups,
  listVpcs,
  replaceSecurityGroupRules
} from "@/shared/lib/api/client";
import { buttonStyle, dangerButtonStyle, errorStyle, inlineButtonStyle, inputStyle, panelStyle, secondaryButtonStyle, subTextStyle } from "@/features/network/shared/panelStyles";

interface SecurityGroupPanelProps {
  selectedProject: ProjectResponse | null;
}

interface RuleDraft {
  direction: NetworkRuleDirection;
  protocol: NetworkRuleProtocol;
  portRangeStart: number | null;
  portRangeEnd: number | null;
  cidrBlock: string;
  description: string | null;
}

const protocols: NetworkRuleProtocol[] = ["TCP", "UDP", "ICMP", "ALL"];
const directions: NetworkRuleDirection[] = ["INGRESS", "EGRESS"];

export function SecurityGroupPanel({ selectedProject }: SecurityGroupPanelProps) {
  const session = getAuthSession();
  const [tenantId, setTenantId] = useState(session.tenantId);
  const [vpcs, setVpcs] = useState<VpcResponse[]>([]);
  const [vpcId, setVpcId] = useState("");
  const [name, setName] = useState("web-sg");
  const [description, setDescription] = useState("web access");
  const [groups, setGroups] = useState<SecurityGroupResponse[]>([]);
  const [selectedGroupId, setSelectedGroupId] = useState("");
  const [direction, setDirection] = useState<NetworkRuleDirection>("INGRESS");
  const [protocol, setProtocol] = useState<NetworkRuleProtocol>("TCP");
  const [portStart, setPortStart] = useState("80");
  const [portEnd, setPortEnd] = useState("80");
  const [cidrBlock, setCidrBlock] = useState("0.0.0.0/0");
  const [ruleDescription, setRuleDescription] = useState("http");
  const [draftRules, setDraftRules] = useState<RuleDraft[]>([]);
  const [errorMessage, setErrorMessage] = useState("");

  const refresh = async () => {
    if (!selectedProject) {
      setVpcs([]);
      setGroups([]);
      setVpcId("");
      return;
    }
    const latestVpcs = await listVpcs(tenantId.trim(), selectedProject.id);
    setVpcs(latestVpcs);
    setVpcId((current) => (current && latestVpcs.some((item) => item.id === current) ? current : latestVpcs[0]?.id ?? ""));
    const latestGroups = await listSecurityGroups(tenantId.trim(), selectedProject.id);
    setGroups(latestGroups);
    setSelectedGroupId((current) => (current && latestGroups.some((item) => item.id === current) ? current : latestGroups[0]?.id ?? ""));
  };

  useEffect(() => {
    const run = async () => {
      try {
        if (!selectedProject) {
          setVpcs([]);
          setGroups([]);
          setVpcId("");
          return;
        }
        const latestVpcs = await listVpcs(tenantId.trim(), selectedProject.id);
        setVpcs(latestVpcs);
        setVpcId((current) => (current && latestVpcs.some((item) => item.id === current) ? current : latestVpcs[0]?.id ?? ""));
        const latestGroups = await listSecurityGroups(tenantId.trim(), selectedProject.id);
        setGroups(latestGroups);
        setSelectedGroupId((current) => (current && latestGroups.some((item) => item.id === current) ? current : latestGroups[0]?.id ?? ""));
      } catch (error) {
        setErrorMessage(error instanceof Error ? error.message : "Security Group 조회에 실패했습니다.");
      }
    };
    void run();
  }, [selectedProject, tenantId]);

  const handleCreate = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!selectedProject || !vpcId) {
      setErrorMessage("프로젝트와 VPC를 먼저 선택하세요.");
      return;
    }
    setErrorMessage("");
    try {
      await createSecurityGroup(tenantId.trim(), selectedProject.id, vpcId, name.trim(), description.trim());
      await refresh();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Security Group 생성에 실패했습니다.");
    }
  };

  const addRule = () => {
    const start = portStart.trim() ? Number(portStart) : null;
    const end = portEnd.trim() ? Number(portEnd) : null;
    setDraftRules((current) => [
      ...current,
      {
        direction,
        protocol,
        portRangeStart: start,
        portRangeEnd: end,
        cidrBlock: cidrBlock.trim(),
        description: ruleDescription.trim() || null
      }
    ]);
  };

  const handleReplaceRules = async () => {
    if (!selectedGroupId) {
      setErrorMessage("Security Group을 먼저 선택하세요.");
      return;
    }
    setErrorMessage("");
    try {
      await replaceSecurityGroupRules(selectedGroupId, draftRules);
      setDraftRules([]);
      await refresh();
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "규칙 교체에 실패했습니다.");
    }
  };

  return (
    <section style={panelStyle}>
      <h2 style={{ marginTop: 0 }}>Security Group 운영</h2>
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
        <input value={name} onChange={(event) => setName(event.target.value)} placeholder="Security Group Name" required style={inputStyle} />
        <input value={description} onChange={(event) => setDescription(event.target.value)} placeholder="Description" style={inputStyle} />
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <button type="submit" style={buttonStyle}>
            SG 생성
          </button>
          <button type="button" style={secondaryButtonStyle} onClick={() => void refresh()}>
            SG 조회
          </button>
        </div>
      </form>

      <div style={{ display: "grid", gap: 8, marginTop: 12 }}>
        <select value={selectedGroupId} onChange={(event) => setSelectedGroupId(event.target.value)} style={inputStyle}>
          <option value="">규칙 편집할 SG 선택</option>
          {groups.map((group) => (
            <option key={group.id} value={group.id}>
              {group.name}
            </option>
          ))}
        </select>
        <select value={direction} onChange={(event) => setDirection(event.target.value as NetworkRuleDirection)} style={inputStyle}>
          {directions.map((item) => (
            <option key={item} value={item}>
              {item}
            </option>
          ))}
        </select>
        <select value={protocol} onChange={(event) => setProtocol(event.target.value as NetworkRuleProtocol)} style={inputStyle}>
          {protocols.map((item) => (
            <option key={item} value={item}>
              {item}
            </option>
          ))}
        </select>
        <input value={portStart} onChange={(event) => setPortStart(event.target.value)} placeholder="Port Start" style={inputStyle} />
        <input value={portEnd} onChange={(event) => setPortEnd(event.target.value)} placeholder="Port End" style={inputStyle} />
        <input value={cidrBlock} onChange={(event) => setCidrBlock(event.target.value)} placeholder="CIDR" style={inputStyle} />
        <input value={ruleDescription} onChange={(event) => setRuleDescription(event.target.value)} placeholder="Rule Description" style={inputStyle} />
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <button type="button" style={inlineButtonStyle} onClick={addRule}>
            규칙 초안 추가
          </button>
          <button type="button" style={buttonStyle} onClick={() => void handleReplaceRules()}>
            규칙 전체 교체
          </button>
        </div>
      </div>

      {draftRules.length ? (
        <ul style={{ marginTop: 12, paddingLeft: 16 }}>
          {draftRules.map((rule, index) => (
            <li key={`${rule.direction}-${rule.protocol}-${index}`}>
              {rule.direction} {rule.protocol} {rule.portRangeStart ?? "-"}-{rule.portRangeEnd ?? "-"} {rule.cidrBlock}
            </li>
          ))}
        </ul>
      ) : null}

      <ul style={{ marginTop: 14, paddingLeft: 16 }}>
        {groups.map((group) => (
          <li key={group.id} style={{ marginBottom: 12 }}>
            <strong>{group.name}</strong> / rules {group.rules.length}
            <div style={subTextStyle}>{group.description ?? "description 없음"}</div>
            <ul style={{ marginTop: 6, paddingLeft: 16 }}>
              {group.rules.map((rule) => (
                <li key={rule.id}>
                  {rule.direction} {rule.protocol} {rule.portRangeStart ?? "-"}-{rule.portRangeEnd ?? "-"} {rule.cidrBlock}
                </li>
              ))}
            </ul>
            <button
              type="button"
              style={dangerButtonStyle}
              onClick={() => void deleteSecurityGroup(group.id).then(refresh).catch((error: unknown) => {
                setErrorMessage(error instanceof Error ? error.message : "Security Group 삭제에 실패했습니다.");
              })}
            >
              Delete SG
            </button>
          </li>
        ))}
      </ul>

      {errorMessage ? <p style={errorStyle}>{errorMessage}</p> : null}
    </section>
  );
}
