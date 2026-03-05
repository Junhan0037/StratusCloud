export type RoleType = "OWNER" | "ADMIN" | "DEVELOPER" | "VIEWER";
export type ApiKeyStatus = "ACTIVE" | "REVOKED";

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
