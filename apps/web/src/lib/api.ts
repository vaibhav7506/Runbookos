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
  IntegrationView,
  IntegrationSetupRequest,
  IntegrationUsageView,
  PostmortemView,
  OperationsOverview,
  AuditPage,
  DeadLetterView,
  PolicyPreview,
} from "@runbookos/api-client";

const LOCAL_API_URL = "http://localhost:8080";
let accessToken: string | null = null;

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
    public readonly correlationId?: string,
    public readonly validationErrors: Array<{ field: string; message: string }> = [],
    public readonly networkFailure = false
  ) {
    super(message);
    this.name = "ApiError";
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

type RequestOptions = Omit<RequestInit, "body"> & {
  body?: BodyInit | object | null;
  requiresAuth?: boolean;
};

function apiBaseUrl(): string {
  const configured =
    typeof window === "undefined"
      ? (process.env.INTERNAL_API_URL ?? process.env.NEXT_PUBLIC_API_URL)
      : process.env.NEXT_PUBLIC_API_URL;
  return (configured ?? LOCAL_API_URL).replace(/\/+$/, "");
}

function isPlainJsonBody(body: RequestOptions["body"]): body is object {
  if (body === null || typeof body !== "object") return false;
  if (Array.isArray(body)) return true;
  return Object.getPrototypeOf(body) === Object.prototype || Object.getPrototypeOf(body) === null;
}

async function parseResponse(response: Response): Promise<unknown> {
  if (response.status === 204 || response.status === 205) return undefined;
  const text = await response.text();
  if (!text.trim()) return undefined;
  if (response.headers.get("content-type")?.toLowerCase().includes("json")) {
    try {
      return JSON.parse(text) as unknown;
    } catch {
      return text;
    }
  }
  return text;
}

function asErrorResponse(value: unknown): ErrorResponse | null {
  return value !== null && typeof value === "object" ? (value as ErrorResponse) : null;
}

export async function request<T>(
  path: string,
  options: RequestOptions = {},
  retry = true
): Promise<T> {
  const { body, requiresAuth = true, ...init } = options;
  const headers = new Headers(init.headers);
  const jsonBody = isPlainJsonBody(body);
  if (jsonBody && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  const currentToken = requiresAuth ? token() : null;
  if (currentToken) headers.set("Authorization", `Bearer ${currentToken}`);

  let response: Response;
  try {
    response = await fetch(`${apiBaseUrl()}${path}`, {
      ...init,
      body: jsonBody ? JSON.stringify(body) : (body as BodyInit | null | undefined),
      headers,
      credentials: "include",
    });
  } catch {
    throw new ApiError(
      0,
      "BACKEND_UNAVAILABLE",
      "RunbookOS backend is unavailable. Start the control-plane service and try again.",
      undefined,
      [],
      true
    );
  }

  if (response.status === 401 && retry && requiresAuth) {
    try {
      const auth = await request<AuthResponse>(
        "/api/auth/refresh",
        { method: "POST", requiresAuth: false },
        false
      );
      setAccessToken(auth.accessToken);
      return request<T>(path, options, false);
    } catch {
      setAccessToken(null);
      if (typeof window !== "undefined") {
        window.dispatchEvent(new Event("runbookos:session-expired"));
      }
    }
  }

  const parsed = await parseResponse(response);
  if (!response.ok) {
    const error = asErrorResponse(parsed);
    throw new ApiError(
      response.status,
      error?.code ?? "REQUEST_FAILED",
      error?.message ??
        (typeof parsed === "string" && parsed.trim()
          ? parsed
          : `Request failed (${response.status})`),
      error?.correlationId,
      error?.validationErrors?.map((item) => ({
        field: item.field,
        message: item.message,
      })) ?? []
    );
  }
  return parsed as T;
}

export const api = {
  currentUser: () => request<CurrentUser>("/api/auth/me"),
  login: (email: string, password: string) =>
    request<AuthResponse>("/api/auth/login", {
      method: "POST",
      body: { email, password },
      requiresAuth: false,
    }).then((value) => {
      setAccessToken(value.accessToken);
      return value;
    }),
  signup: (email: string, password: string, displayName: string) =>
    request<AuthResponse>("/api/auth/signup", {
      method: "POST",
      body: { email, password, displayName },
      requiresAuth: false,
    }).then((value) => {
      setAccessToken(value.accessToken);
      return value;
    }),
  createOrganization: (name: string, demoMode: boolean) =>
    request<{ id: string; name: string; slug: string; demoMode: boolean }>("/api/organizations", {
      method: "POST",
      body: { name, demoMode },
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
      body: { status },
    }),
  comment: (id: string, body: string) =>
    request(`/api/incidents/${id}/comments`, { method: "POST", body: { body } }),
  createExecution: (incidentId: string) =>
    request<ExecutionView>("/api/executions", {
      method: "POST",
      headers: { "Idempotency-Key": crypto.randomUUID() },
      body: { incidentId, workflowKey: "incident-triage" },
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
      body: value,
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
      body: { name, description },
    }),
  replaceRunbookSteps: (id: string, versionId: string, value: StepsRequest) =>
    request<RunbookDetail>(`/api/runbooks/${id}/versions/${versionId}/steps`, {
      method: "PUT",
      body: value,
    }),
  previewRunbook: (id: string, versionId: string, environment: string) =>
    request<PolicyPreview[]>(`/api/runbooks/${id}/versions/${versionId}/policy-preview`, {
      method: "POST",
      body: { environment },
    }),
  publishRunbook: (id: string, versionId: string, environment: string) =>
    request<RunbookDetail>(`/api/runbooks/${id}/versions/${versionId}/publish`, {
      method: "POST",
      body: { environment },
    }),
  policies: () => request<PolicyView[]>("/api/policies"),
  updatePolicyRule: (policyId: string, risk: string, value: RuleRequest) =>
    request<PolicyView>(`/api/policies/${policyId}/rules/${risk}`, {
      method: "PUT",
      body: value,
    }),
  approvals: (status?: string) =>
    request<ApprovalView[]>(`/api/approvals${status ? `?status=${status}` : ""}`),
  approval: (id: string) => request<ApprovalView>(`/api/approvals/${id}`),
  decideApproval: (id: string, value: DecisionRequest) =>
    request<ApprovalView>(`/api/approvals/${id}/decisions`, {
      method: "POST",
      body: value,
    }),
  integrations: () => request<IntegrationView[]>("/api/integrations"),
  createIntegration: (value: IntegrationSetupRequest) =>
    request<IntegrationView>("/api/integrations", {
      method: "POST",
      body: value,
    }),
  validateIntegration: (id: string) =>
    request<IntegrationView>(`/api/integrations/${id}/validate`, { method: "POST" }),
  disconnectIntegration: (id: string) =>
    request<void>(`/api/integrations/${id}`, { method: "DELETE" }),
  integrationUsage: (id: string) =>
    request<IntegrationUsageView[]>(`/api/integrations/${id}/usage`),
  postmortem: (incidentId: string) =>
    request<PostmortemView>(`/api/incidents/${incidentId}/postmortem`),
  generatePostmortem: (incidentId: string) =>
    request<PostmortemView>(`/api/incidents/${incidentId}/postmortem`, { method: "POST" }),
  operationsOverview: () => request<OperationsOverview>("/api/operations/overview"),
  auditEvents: (page = 0, size = 25) => request<AuditPage>(`/api/audit?page=${page}&size=${size}`),
  deadLetters: () => request<DeadLetterView[]>("/api/operations/dead-letters"),
  redriveDeadLetter: (id: string) =>
    request<DeadLetterView>(`/api/operations/dead-letters/${id}/redrive`, { method: "POST" }),
};

export function authorizedStream(path: string, signal: AbortSignal) {
  const currentToken = token();
  return fetch(`${apiBaseUrl()}${path}`, {
    headers: currentToken ? { Authorization: `Bearer ${currentToken}` } : undefined,
    credentials: "include",
    signal,
  });
}
