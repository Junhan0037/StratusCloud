export type RoleType = "OWNER" | "ADMIN" | "DEVELOPER" | "VIEWER";

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

interface ApiErrorResponse {
  code: string;
  message: string;
  traceId: string;
  details?: Record<string, unknown>;
}

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

function buildHeaders(role: RoleType = "ADMIN"): HeadersInit {
  // 한국어 설명: Week 2에서는 임시 RBAC 헤더를 공통으로 넣어 API 호출을 통일한다.
  return {
    "Content-Type": "application/json",
    "X-Project-Role": role,
    "X-Actor-Id": crypto.randomUUID()
  };
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
    headers: buildHeaders("ADMIN"),
    body: JSON.stringify({ tenantId, name })
  });
}

export async function getProject(projectId: string): Promise<ProjectResponse> {
  return request<ProjectResponse>(`/v1/projects/${projectId}`, {
    method: "GET",
    headers: buildHeaders("VIEWER")
  });
}

export async function createUser(email: string, displayName: string): Promise<UserResponse> {
  return request<UserResponse>("/v1/iam/users", {
    method: "POST",
    headers: buildHeaders("ADMIN"),
    body: JSON.stringify({ email, displayName })
  });
}

export async function addMember(projectId: string, userId: string, role: RoleType): Promise<MembershipResponse> {
  return request<MembershipResponse>(`/v1/projects/${projectId}/members`, {
    method: "POST",
    headers: buildHeaders("ADMIN"),
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
    headers: buildHeaders("ADMIN"),
    body: JSON.stringify({ role })
  });
}
