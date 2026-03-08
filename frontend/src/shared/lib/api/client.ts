export type RoleType = "OWNER" | "ADMIN" | "DEVELOPER" | "VIEWER";
export type ApiKeyStatus = "ACTIVE" | "REVOKED";
export type SecretVersionStatus = "ACTIVE" | "REVOKED";
export type AuditResult = "SUCCESS" | "DENIED";
export type ComputeImageStatus = "ACTIVE" | "DEPRECATED";
export type ComputeInstanceStatus = "PENDING" | "RUNNING" | "STOPPED" | "TERMINATED";
export type ComputeOsType = "LINUX" | "WINDOWS";
export type ComputeHealthStatus = "UNKNOWN" | "HEALTHY" | "UNHEALTHY";
export type ComputeHealthPolicy = "RESTART";
export type ComputeAutoscalingGroupStatus = "ACTIVE";
export type NetworkRouteTargetType = "LOCAL" | "INTERNET_GATEWAY";
export type NetworkRuleDirection = "INGRESS" | "EGRESS";
export type NetworkRuleProtocol = "TCP" | "UDP" | "ICMP" | "ALL";

export interface ProjectResponse {
  id: string;
  tenantId: string;
  name: string;
  createdAt: string;
}

export interface UserResponse {
  id: string;
  email: string;
  displayName: string;
  createdAt: string;
}

export interface MembershipResponse {
  id: string;
  tenantId: string;
  projectId: string;
  userId: string;
  role: RoleType;
  createdAt: string;
}

export interface PolicyResponse {
  id: string;
  tenantId: string;
  name: string;
  description: string | null;
  document: {
    version: string;
    statements: Array<{
      effect: "ALLOW" | "DENY";
      actions: string[];
      resources: string[];
    }>;
  };
  createdAt: string;
}

export interface RolePolicyResponse {
  id: string;
  tenantId: string;
  role: RoleType;
  policyId: string;
  createdAt: string;
}

export interface ApiKeyResponse {
  id: string;
  tenantId: string;
  projectId: string | null;
  name: string;
  role: RoleType;
  keyPrefix: string;
  status: ApiKeyStatus;
  expiresAt: string;
  revokedAt: string | null;
  createdAt: string;
  rawKey?: string;
}

export interface SecretVersionResponse {
  id: string;
  version: number;
  status: SecretVersionStatus;
  revokedAt: string | null;
  createdAt: string;
  value?: string;
}

export interface SecretResponse {
  id: string;
  tenantId: string;
  projectId: string | null;
  name: string;
  latestVersion: number;
  createdAt: string;
  currentVersion?: SecretVersionResponse | null;
}

export interface AuditLogResponse {
  id: string;
  traceId: string;
  actorId: string;
  tenantId: string;
  projectId: string | null;
  action: string;
  resourceType: string;
  resourceId: string | null;
  result: AuditResult;
  metadata: Record<string, unknown>;
  occurredAt: string;
}

export interface ComputeImageResponse {
  id: string;
  tenantId: string;
  name: string;
  version: string;
  osType: ComputeOsType;
  status: ComputeImageStatus;
  tags: string[];
  createdAt: string;
}

export interface ComputeInstanceResponse {
  id: string;
  tenantId: string;
  projectId: string;
  imageId: string;
  autoscalingGroupId: string | null;
  name: string;
  flavor: string;
  status: ComputeInstanceStatus;
  healthStatus: ComputeHealthStatus;
  restartCount: number;
  latestMetric: ComputeInstanceMetricResponse | null;
  userData: string | null;
  createdAt: string;
  updatedAt: string;
  lastTransitionAt: string;
}

export interface ComputeInstanceMetricResponse {
  instanceId: string;
  cpuPercent: number;
  memoryPercent: number;
  reportedAt: string;
}

export interface ComputeInstanceHealthResponse {
  instanceId: string;
  status: ComputeHealthStatus;
  failureCount: number;
  detail: string | null;
  checkedAt: string;
}

export interface ComputeAutoscalingGroupResponse {
  id: string;
  tenantId: string;
  projectId: string;
  name: string;
  imageId: string;
  flavor: string;
  minInstances: number;
  maxInstances: number;
  desiredInstances: number;
  currentInstances: number;
  cpuScaleOutPercent: number;
  cpuScaleInPercent: number;
  memoryScaleOutPercent: number;
  memoryScaleInPercent: number;
  averageCpuPercent: number | null;
  averageMemoryPercent: number | null;
  healthPolicy: ComputeHealthPolicy;
  failureThreshold: number;
  status: ComputeAutoscalingGroupStatus;
  createdAt: string;
  updatedAt: string;
}

export interface ComputeHealthReconcileResponse {
  restartedInstanceIds: string[];
  replacementInstanceIds: string[];
}

export interface VpcResponse {
  id: string;
  tenantId: string;
  projectId: string;
  name: string;
  cidrBlock: string;
  defaultRouteTableId: string;
  createdAt: string;
  updatedAt: string;
}

export interface SubnetResponse {
  id: string;
  tenantId: string;
  projectId: string;
  vpcId: string;
  name: string;
  cidrBlock: string;
  availabilityZone: string;
  routeTableId: string | null;
  routeTableAssociationId: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface RouteResponse {
  id: string;
  destinationCidr: string;
  targetType: NetworkRouteTargetType;
  createdAt: string;
}

export interface RouteTableAssociationResponse {
  id: string;
  routeTableId: string;
  subnetId: string;
  createdAt: string;
}

export interface RouteTableResponse {
  id: string;
  tenantId: string;
  projectId: string;
  vpcId: string;
  name: string;
  isDefault: boolean;
  routes: RouteResponse[];
  associations: RouteTableAssociationResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface SecurityGroupRuleResponse {
  id: string;
  direction: NetworkRuleDirection;
  protocol: NetworkRuleProtocol;
  portRangeStart: number | null;
  portRangeEnd: number | null;
  cidrBlock: string;
  description: string | null;
  createdAt: string;
}

export interface SecurityGroupResponse {
  id: string;
  tenantId: string;
  projectId: string;
  vpcId: string;
  name: string;
  description: string | null;
  rules: SecurityGroupRuleResponse[];
  createdAt: string;
  updatedAt: string;
}

export interface AuthSession {
  bearerToken: string;
  apiKey: string;
  tenantId: string;
}

interface ApiErrorResponse {
  code: string;
  message: string;
  traceId: string;
  details?: Record<string, unknown>;
}

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

let authSession: AuthSession = {
  bearerToken: "",
  apiKey: "",
  tenantId: ""
};

export function setAuthSession(next: Partial<AuthSession>): AuthSession {
  authSession = {
    ...authSession,
    ...next
  };
  return authSession;
}

export function getAuthSession(): AuthSession {
  return authSession;
}

function buildHeaders(): HeadersInit {
  const headers: HeadersInit = {
    "Content-Type": "application/json"
  };
  // JWT가 있으면 우선 사용하고, 없을 때만 API Key 인증으로 요청한다.
  if (authSession.bearerToken.trim()) {
    headers.Authorization = `Bearer ${authSession.bearerToken.trim()}`;
  } else if (authSession.apiKey.trim()) {
    headers["X-API-Key"] = authSession.apiKey.trim();
  }
  return headers;
}

async function request<T>(path: string, init: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, init);

  if (!response.ok) {
    const fallback = {
      code: "UNKNOWN_ERROR",
      message: "요청 처리 중 오류가 발생했습니다.",
      traceId: "unknown"
    } satisfies ApiErrorResponse;

    const payload = (await response.json().catch(() => fallback)) as ApiErrorResponse;
    throw new Error(`[${payload.code}] ${payload.message} (traceId=${payload.traceId})`);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export async function createProject(tenantId: string, name: string): Promise<ProjectResponse> {
  return request<ProjectResponse>("/v1/projects", {
    method: "POST",
    headers: buildHeaders(),
    body: JSON.stringify({ tenantId, name })
  });
}

export async function getProject(projectId: string): Promise<ProjectResponse> {
  return request<ProjectResponse>(`/v1/projects/${projectId}`, {
    method: "GET",
    headers: buildHeaders()
  });
}

export async function createUser(email: string, displayName: string): Promise<UserResponse> {
  return request<UserResponse>("/v1/iam/users", {
    method: "POST",
    headers: buildHeaders(),
    body: JSON.stringify({ email, displayName })
  });
}

export async function addMember(projectId: string, userId: string, role: RoleType): Promise<MembershipResponse> {
  return request<MembershipResponse>(`/v1/projects/${projectId}/members`, {
    method: "POST",
    headers: buildHeaders(),
    body: JSON.stringify({ userId, role })
  });
}

export async function updateMemberRole(
  projectId: string,
  userId: string,
  role: RoleType
): Promise<MembershipResponse> {
  return request<MembershipResponse>(`/v1/projects/${projectId}/members/${userId}/role`, {
    method: "PATCH",
    headers: buildHeaders(),
    body: JSON.stringify({ role })
  });
}

export async function createPolicy(
  tenantId: string,
  name: string,
  description: string,
  action: string,
  resource: string
): Promise<PolicyResponse> {
  return request<PolicyResponse>("/v1/iam/policies", {
    method: "POST",
    headers: buildHeaders(),
    body: JSON.stringify({
      tenantId,
      name,
      description,
      document: {
        version: "2026-03-05",
        statements: [
          {
            effect: "ALLOW",
            actions: [action],
            resources: [resource]
          }
        ]
      }
    })
  });
}

export async function listPolicies(tenantId: string): Promise<PolicyResponse[]> {
  const encodedTenantId = encodeURIComponent(tenantId);
  return request<PolicyResponse[]>(`/v1/iam/policies?tenantId=${encodedTenantId}`, {
    method: "GET",
    headers: buildHeaders()
  });
}

export async function bindRolePolicies(
  tenantId: string,
  role: RoleType,
  policyIds: string[]
): Promise<RolePolicyResponse[]> {
  return request<RolePolicyResponse[]>("/v1/iam/roles", {
    method: "POST",
    headers: buildHeaders(),
    body: JSON.stringify({
      tenantId,
      role,
      policyIds
    })
  });
}

export async function createApiKey(
  tenantId: string,
  name: string,
  role: RoleType,
  projectId?: string
): Promise<ApiKeyResponse> {
  return request<ApiKeyResponse>("/v1/iam/api-keys", {
    method: "POST",
    headers: buildHeaders(),
    body: JSON.stringify({
      tenantId,
      name,
      role,
      projectId: projectId?.trim() ? projectId.trim() : null
    })
  });
}

export async function listApiKeys(tenantId: string): Promise<ApiKeyResponse[]> {
  const encodedTenantId = encodeURIComponent(tenantId);
  return request<ApiKeyResponse[]>(`/v1/iam/api-keys?tenantId=${encodedTenantId}`, {
    method: "GET",
    headers: buildHeaders()
  });
}

export async function revokeApiKey(keyId: string): Promise<ApiKeyResponse> {
  return request<ApiKeyResponse>(`/v1/iam/api-keys/${keyId}`, {
    method: "DELETE",
    headers: buildHeaders()
  });
}

export async function createSecret(
  tenantId: string,
  name: string,
  value: string,
  projectId?: string
): Promise<SecretResponse> {
  return request<SecretResponse>("/v1/iam/secrets", {
    method: "POST",
    headers: buildHeaders(),
    body: JSON.stringify({
      tenantId,
      projectId: projectId?.trim() ? projectId.trim() : null,
      name,
      value
    })
  });
}

export async function listSecrets(tenantId: string, projectId?: string): Promise<SecretResponse[]> {
  const params = new URLSearchParams({ tenantId });
  if (projectId?.trim()) {
    params.set("projectId", projectId.trim());
  }
  return request<SecretResponse[]>(`/v1/iam/secrets?${params.toString()}`, {
    method: "GET",
    headers: buildHeaders()
  });
}

export async function listSecretVersions(secretId: string): Promise<SecretVersionResponse[]> {
  return request<SecretVersionResponse[]>(`/v1/iam/secrets/${secretId}/versions`, {
    method: "GET",
    headers: buildHeaders()
  });
}

export async function rotateSecret(secretId: string, value: string): Promise<SecretResponse> {
  return request<SecretResponse>(`/v1/iam/secrets/${secretId}:rotate`, {
    method: "POST",
    headers: buildHeaders(),
    body: JSON.stringify({ value })
  });
}

export async function revokeSecretVersion(secretId: string, versionId: string): Promise<SecretVersionResponse> {
  return request<SecretVersionResponse>(`/v1/iam/secrets/${secretId}/versions/${versionId}`, {
    method: "DELETE",
    headers: buildHeaders()
  });
}

interface AuditLogFilter {
  tenantId: string;
  projectId?: string;
  actorId?: string;
  resourceType?: string;
  action?: string;
  result?: AuditResult | "";
}

interface ComputeImageFilter {
  tenantId: string;
  status?: ComputeImageStatus | "";
  osType?: ComputeOsType | "";
  tag?: string;
}

interface ComputeAutoscalingGroupFilter {
  tenantId: string;
  projectId: string;
}

export async function listAuditLogs(filters: AuditLogFilter): Promise<AuditLogResponse[]> {
  const params = new URLSearchParams({ tenantId: filters.tenantId });
  if (filters.projectId?.trim()) {
    params.set("projectId", filters.projectId.trim());
  }
  if (filters.actorId?.trim()) {
    params.set("actorId", filters.actorId.trim());
  }
  if (filters.resourceType?.trim()) {
    params.set("resourceType", filters.resourceType.trim());
  }
  if (filters.action?.trim()) {
    params.set("action", filters.action.trim());
  }
  if (filters.result?.trim()) {
    params.set("result", filters.result.trim());
  }

  return request<AuditLogResponse[]>(`/v1/audit/logs?${params.toString()}`, {
    method: "GET",
    headers: buildHeaders()
  });
}

export async function createComputeImage(
  tenantId: string,
  name: string,
  version: string,
  osType: ComputeOsType,
  tags: string[],
  status: ComputeImageStatus = "ACTIVE"
): Promise<ComputeImageResponse> {
  return request<ComputeImageResponse>("/v1/compute/images", {
    method: "POST",
    headers: buildHeaders(),
    body: JSON.stringify({
      tenantId,
      name,
      version,
      osType,
      status,
      tags
    })
  });
}

export async function listComputeImages(filters: ComputeImageFilter): Promise<ComputeImageResponse[]> {
  const params = new URLSearchParams({ tenantId: filters.tenantId });
  if (filters.status?.trim()) {
    params.set("status", filters.status.trim());
  }
  if (filters.osType?.trim()) {
    params.set("osType", filters.osType.trim());
  }
  if (filters.tag?.trim()) {
    params.set("tag", filters.tag.trim());
  }
  return request<ComputeImageResponse[]>(`/v1/compute/images?${params.toString()}`, {
    method: "GET",
    headers: buildHeaders()
  });
}

export async function createComputeInstance(
  tenantId: string,
  projectId: string,
  name: string,
  imageId: string,
  flavor: string,
  userData?: string
): Promise<ComputeInstanceResponse> {
  return request<ComputeInstanceResponse>("/v1/compute/instances", {
    method: "POST",
    headers: buildHeaders(),
    body: JSON.stringify({
      tenantId,
      projectId,
      name,
      imageId,
      flavor,
      userData: userData?.trim() ? userData : null
    })
  });
}

export async function listComputeInstances(tenantId: string, projectId: string): Promise<ComputeInstanceResponse[]> {
  const params = new URLSearchParams({ tenantId, projectId });
  return request<ComputeInstanceResponse[]>(`/v1/compute/instances?${params.toString()}`, {
    method: "GET",
    headers: buildHeaders()
  });
}

export async function startComputeInstance(instanceId: string): Promise<ComputeInstanceResponse> {
  return request<ComputeInstanceResponse>(`/v1/compute/instances/${instanceId}:start`, {
    method: "POST",
    headers: buildHeaders()
  });
}

export async function stopComputeInstance(instanceId: string): Promise<ComputeInstanceResponse> {
  return request<ComputeInstanceResponse>(`/v1/compute/instances/${instanceId}:stop`, {
    method: "POST",
    headers: buildHeaders()
  });
}

export async function terminateComputeInstance(instanceId: string): Promise<ComputeInstanceResponse> {
  return request<ComputeInstanceResponse>(`/v1/compute/instances/${instanceId}`, {
    method: "DELETE",
    headers: buildHeaders()
  });
}

export async function createComputeAutoscalingGroup(
  tenantId: string,
  projectId: string,
  name: string,
  imageId: string,
  flavor: string,
  minInstances: number,
  maxInstances: number,
  cpuScaleOutPercent: number,
  cpuScaleInPercent: number,
  memoryScaleOutPercent: number,
  memoryScaleInPercent: number,
  failureThreshold = 3,
  healthPolicy: ComputeHealthPolicy = "RESTART"
): Promise<ComputeAutoscalingGroupResponse> {
  return request<ComputeAutoscalingGroupResponse>("/v1/compute/autoscaling-groups", {
    method: "POST",
    headers: buildHeaders(),
    body: JSON.stringify({
      tenantId,
      projectId,
      name,
      imageId,
      flavor,
      minInstances,
      maxInstances,
      cpuScaleOutPercent,
      cpuScaleInPercent,
      memoryScaleOutPercent,
      memoryScaleInPercent,
      failureThreshold,
      healthPolicy
    })
  });
}

export async function listComputeAutoscalingGroups(
  filters: ComputeAutoscalingGroupFilter
): Promise<ComputeAutoscalingGroupResponse[]> {
  const params = new URLSearchParams({ tenantId: filters.tenantId, projectId: filters.projectId });
  return request<ComputeAutoscalingGroupResponse[]>(`/v1/compute/autoscaling-groups?${params.toString()}`, {
    method: "GET",
    headers: buildHeaders()
  });
}

export async function evaluateComputeAutoscalingGroup(groupId: string): Promise<ComputeAutoscalingGroupResponse> {
  return request<ComputeAutoscalingGroupResponse>(`/v1/compute/autoscaling-groups/${groupId}:evaluate`, {
    method: "POST",
    headers: buildHeaders()
  });
}

export async function reconcileComputeGroupHealth(groupId: string): Promise<ComputeHealthReconcileResponse> {
  return request<ComputeHealthReconcileResponse>(`/v1/compute/autoscaling-groups/${groupId}:reconcile-health`, {
    method: "POST",
    headers: buildHeaders()
  });
}

export async function writeComputeInstanceMetric(
  instanceId: string,
  cpuPercent: number,
  memoryPercent: number
): Promise<ComputeInstanceMetricResponse> {
  return request<ComputeInstanceMetricResponse>(`/v1/compute/instances/${instanceId}/metrics`, {
    method: "POST",
    headers: buildHeaders(),
    body: JSON.stringify({ cpuPercent, memoryPercent })
  });
}

export async function writeComputeInstanceHealth(
  instanceId: string,
  status: ComputeHealthStatus,
  detail?: string
): Promise<ComputeInstanceHealthResponse> {
  return request<ComputeInstanceHealthResponse>(`/v1/compute/instances/${instanceId}/health`, {
    method: "POST",
    headers: buildHeaders(),
    body: JSON.stringify({
      status,
      detail: detail?.trim() ? detail : null
    })
  });
}

export async function createVpc(
  tenantId: string,
  projectId: string,
  name: string,
  cidrBlock: string
): Promise<VpcResponse> {
  return request<VpcResponse>("/v1/network/vpcs", {
    method: "POST",
    headers: buildHeaders(),
    body: JSON.stringify({ tenantId, projectId, name, cidrBlock })
  });
}

export async function listVpcs(tenantId: string, projectId: string): Promise<VpcResponse[]> {
  const params = new URLSearchParams({ tenantId, projectId });
  return request<VpcResponse[]>(`/v1/network/vpcs?${params.toString()}`, {
    method: "GET",
    headers: buildHeaders()
  });
}

export async function deleteVpc(vpcId: string): Promise<void> {
  return request<void>(`/v1/network/vpcs/${vpcId}`, {
    method: "DELETE",
    headers: buildHeaders()
  });
}

export async function createSubnet(
  tenantId: string,
  projectId: string,
  vpcId: string,
  name: string,
  cidrBlock: string,
  availabilityZone: string
): Promise<SubnetResponse> {
  return request<SubnetResponse>("/v1/network/subnets", {
    method: "POST",
    headers: buildHeaders(),
    body: JSON.stringify({ tenantId, projectId, vpcId, name, cidrBlock, availabilityZone })
  });
}

export async function listSubnets(tenantId: string, projectId: string, vpcId?: string): Promise<SubnetResponse[]> {
  const params = new URLSearchParams({ tenantId, projectId });
  if (vpcId?.trim()) {
    params.set("vpcId", vpcId.trim());
  }
  return request<SubnetResponse[]>(`/v1/network/subnets?${params.toString()}`, {
    method: "GET",
    headers: buildHeaders()
  });
}

export async function deleteSubnet(subnetId: string): Promise<void> {
  return request<void>(`/v1/network/subnets/${subnetId}`, {
    method: "DELETE",
    headers: buildHeaders()
  });
}

export async function createRouteTable(
  tenantId: string,
  projectId: string,
  vpcId: string,
  name: string
): Promise<RouteTableResponse> {
  return request<RouteTableResponse>("/v1/network/route-tables", {
    method: "POST",
    headers: buildHeaders(),
    body: JSON.stringify({ tenantId, projectId, vpcId, name })
  });
}

export async function listRouteTables(vpcId: string): Promise<RouteTableResponse[]> {
  const params = new URLSearchParams({ vpcId });
  return request<RouteTableResponse[]>(`/v1/network/route-tables?${params.toString()}`, {
    method: "GET",
    headers: buildHeaders()
  });
}

export async function createRoute(
  routeTableId: string,
  destinationCidr: string,
  targetType: NetworkRouteTargetType
): Promise<RouteResponse> {
  return request<RouteResponse>(`/v1/network/route-tables/${routeTableId}/routes`, {
    method: "POST",
    headers: buildHeaders(),
    body: JSON.stringify({ destinationCidr, targetType })
  });
}

export async function deleteRoute(routeId: string): Promise<void> {
  return request<void>(`/v1/network/routes/${routeId}`, {
    method: "DELETE",
    headers: buildHeaders()
  });
}

export async function associateSubnet(routeTableId: string, subnetId: string): Promise<RouteTableAssociationResponse> {
  return request<RouteTableAssociationResponse>(`/v1/network/route-tables/${routeTableId}/associations`, {
    method: "POST",
    headers: buildHeaders(),
    body: JSON.stringify({ subnetId })
  });
}

export async function deleteRouteTable(routeTableId: string): Promise<void> {
  return request<void>(`/v1/network/route-tables/${routeTableId}`, {
    method: "DELETE",
    headers: buildHeaders()
  });
}

export async function createSecurityGroup(
  tenantId: string,
  projectId: string,
  vpcId: string,
  name: string,
  description: string
): Promise<SecurityGroupResponse> {
  return request<SecurityGroupResponse>("/v1/network/security-groups", {
    method: "POST",
    headers: buildHeaders(),
    body: JSON.stringify({ tenantId, projectId, vpcId, name, description })
  });
}

export async function listSecurityGroups(tenantId: string, projectId: string): Promise<SecurityGroupResponse[]> {
  const params = new URLSearchParams({ tenantId, projectId });
  return request<SecurityGroupResponse[]>(`/v1/network/security-groups?${params.toString()}`, {
    method: "GET",
    headers: buildHeaders()
  });
}

export async function replaceSecurityGroupRules(
  securityGroupId: string,
  rules: Array<{
    direction: NetworkRuleDirection;
    protocol: NetworkRuleProtocol;
    portRangeStart: number | null;
    portRangeEnd: number | null;
    cidrBlock: string;
    description: string | null;
  }>
): Promise<SecurityGroupResponse> {
  return request<SecurityGroupResponse>(`/v1/network/security-groups/${securityGroupId}/rules`, {
    method: "PUT",
    headers: buildHeaders(),
    body: JSON.stringify({ rules })
  });
}

export async function deleteSecurityGroup(securityGroupId: string): Promise<void> {
  return request<void>(`/v1/network/security-groups/${securityGroupId}`, {
    method: "DELETE",
    headers: buildHeaders()
  });
}
