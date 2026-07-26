"use client";

import type {
  AuthResponse,
  CurrentUser,
  ErrorResponse,
  ExecutionTimeline,
  ExecutionView,
  IncidentDetail,
  IncidentPage,
  IncidentView,
  AnalysisView,
  ProviderConfigView,
  ProviderRequest,
  RunbookSummary,
  RunbookDetail,
  StepsRequest,
  PolicyView,
  RuleRequest,
  ApprovalView,
  DecisionRequest,
  PolicyPreview,
} from "@runbookos/api-client";

const baseUrl = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";
let accessToken: string | null = null;

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
    public readonly correlationId?: string
  ) {
    super(message);
  }
}

export function setAccessToken(token: string | null) {
  accessToken = token;
  if (typeof window !== "undefined") {
    if (token) sessionStorage.setItem("runbookos_access", token);
    else sessionStorage.removeItem("runbookos_access");
  }
}

function token() {
  if (!accessToken && typeof window !== "undefined") {
    accessToken = sessionStorage.getItem("runbookos_access");
  }
  return accessToken;
}

async function request<T>(path: string, init: RequestInit = {}, retry = true): Promise<T> {
  const headers = new Headers(init.headers);
  if (init.body) headers.set("Content-Type", "application/json");
  if (token()) headers.set("Authorization", `Bearer ${token()}`);
  const response = await fetch(`${baseUrl}${path}`, { ...init, headers, credentials: "include" });
  if (response.status === 401 && retry && !path.startsWith("/api/auth/")) {
    const refreshed = await fetch(`${baseUrl}/api/auth/refresh`, {
      method: "POST",
      credentials: "include",
    });
    if (refreshed.ok) {
      const auth = (await refreshed.json()) as AuthResponse;
      setAccessToken(auth.accessToken);
      return request<T>(path, init, false);
    }
    setAccessToken(null);
    window.dispatchEvent(new Event("runbookos:session-expired"));
  }
  if (!response.ok) {
    const error = (await response.json().catch(() => null)) as ErrorResponse | null;
    throw new ApiError(
      response.status,
      error?.code ?? "REQUEST_FAILED",
      error?.message ?? "Request failed",
      error?.correlationId
    );
  }
  return response.status === 204 ? (undefined as T) : ((await response.json()) as T);
}

export const api = {
  currentUser: () => request<CurrentUser>("/api/auth/me"),
  login: (email: string, password: string) =>
    request<AuthResponse>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ email, password }),
    }).then((value) => {
      setAccessToken(value.accessToken);
      return value;
    }),
  signup: (email: string, password: string, displayName: string) =>
    request<AuthResponse>("/api/auth/signup", {
      method: "POST",
      body: JSON.stringify({ email, password, displayName }),
    }).then((value) => {
      setAccessToken(value.accessToken);
      return value;
    }),
  createOrganization: (name: string, demoMode: boolean) =>
    request<{ id: string; name: string; slug: string; demoMode: boolean }>("/api/organizations", {
      method: "POST",
      body: JSON.stringify({ name, demoMode }),
    }),
  switchOrganization: (id: string) =>
    request<{ accessToken: string; organizationId: string }>(`/api/organizations/${id}/switch`, {
      method: "POST",
    }).then((value) => {
      setAccessToken(value.accessToken);
      return value;
    }),
  incidents: (params: URLSearchParams) => request<IncidentPage>(`/api/incidents?${params}`),
  incident: (id: string) => request<IncidentDetail>(`/api/incidents/${id}`),
  launchDemo: () => request<IncidentView>("/api/incidents/demo", { method: "POST" }),
  transition: (id: string, status: string) =>
    request<IncidentView>(`/api/incidents/${id}/transitions`, {
      method: "POST",
      body: JSON.stringify({ status }),
    }),
  comment: (id: string, body: string) =>
    request(`/api/incidents/${id}/comments`, { method: "POST", body: JSON.stringify({ body }) }),
  createExecution: (incidentId: string) =>
    request<ExecutionView>("/api/executions", {
      method: "POST",
      headers: { "Idempotency-Key": crypto.randomUUID() },
      body: JSON.stringify({ incidentId, workflowKey: "incident-triage" }),
    }),
  execution: (id: string) => request<ExecutionTimeline>(`/api/executions/${id}`),
  retryExecution: (id: string) =>
    request<ExecutionView>(`/api/executions/${id}/retry`, { method: "POST" }),
  analyses: (incidentId: string) =>
    request<AnalysisView[]>(`/api/incidents/${incidentId}/analyses`),
  analyze: (incidentId: string) =>
    request<AnalysisView>(`/api/incidents/${incidentId}/analyses`, { method: "POST" }),
  aiProviders: () => request<ProviderConfigView[]>("/api/ai/providers"),
  configureAiProvider: (value: ProviderRequest) =>
    request<ProviderConfigView>("/api/ai/providers", {
      method: "POST",
      body: JSON.stringify(value),
    }),
  disableAiProvider: (id: string) => request<void>(`/api/ai/providers/${id}`, { method: "DELETE" }),
  runbooks: () => request<RunbookSummary[]>("/api/runbooks"),
  runbook: (id: string, versionId?: string) =>
    request<RunbookDetail>(
      `/api/runbooks/${id}${versionId ? `?versionId=${encodeURIComponent(versionId)}` : ""}`
    ),
  createRunbook: (name: string, description: string) =>
    request<RunbookDetail>("/api/runbooks", {
      method: "POST",
      body: JSON.stringify({ name, description }),
    }),
  replaceRunbookSteps: (id: string, versionId: string, value: StepsRequest) =>
    request<RunbookDetail>(`/api/runbooks/${id}/versions/${versionId}/steps`, {
      method: "PUT",
      body: JSON.stringify(value),
    }),
  previewRunbook: (id: string, versionId: string, environment: string) =>
    request<PolicyPreview[]>(`/api/runbooks/${id}/versions/${versionId}/policy-preview`, {
      method: "POST",
      body: JSON.stringify({ environment }),
    }),
  publishRunbook: (id: string, versionId: string, environment: string) =>
    request<RunbookDetail>(`/api/runbooks/${id}/versions/${versionId}/publish`, {
      method: "POST",
      body: JSON.stringify({ environment }),
    }),
  policies: () => request<PolicyView[]>("/api/policies"),
  updatePolicyRule: (policyId: string, risk: string, value: RuleRequest) =>
    request<PolicyView>(`/api/policies/${policyId}/rules/${risk}`, {
      method: "PUT",
      body: JSON.stringify(value),
    }),
  approvals: (status?: string) =>
    request<ApprovalView[]>(`/api/approvals${status ? `?status=${status}` : ""}`),
  approval: (id: string) => request<ApprovalView>(`/api/approvals/${id}`),
  decideApproval: (id: string, value: DecisionRequest) =>
    request<ApprovalView>(`/api/approvals/${id}/decisions`, {
      method: "POST",
      body: JSON.stringify(value),
    }),
};

export function authorizedStream(path: string, signal: AbortSignal) {
  return fetch(`${baseUrl}${path}`, {
    headers: { Authorization: `Bearer ${token()}` },
    credentials: "include",
    signal,
  });
}
