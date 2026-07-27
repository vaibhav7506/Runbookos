import { beforeEach, describe, expect, it, vi } from "vitest";
import { api, ApiError, request, setAccessToken } from "./api";

const authResponse = {
  accessToken: "new-access-token",
  expiresIn: 900,
  user: {
    id: "2862539e-1b7f-4f3a-af86-cfc4d66fc761",
    email: "person@example.com",
    displayName: "Test Person",
  },
};

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

function requestHeaders(fetchMock: ReturnType<typeof vi.fn>) {
  const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
  return new Headers(init.headers);
}

describe("API client", () => {
  beforeEach(() => {
    setAccessToken(null);
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it("sends signup without a bearer token", async () => {
    setAccessToken("stale-token");
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(authResponse));
    vi.stubGlobal("fetch", fetchMock);

    await api.signup("person@example.com", "correct-horse-battery", "Test Person");

    expect(requestHeaders(fetchMock).has("Authorization")).toBe(false);
    expect(fetchMock.mock.calls[0]?.[1]).toMatchObject({ credentials: "include" });
  });

  it("sends login without a bearer token", async () => {
    setAccessToken("stale-token");
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(authResponse));
    vi.stubGlobal("fetch", fetchMock);

    await api.login("person@example.com", "correct-horse-battery");

    expect(requestHeaders(fetchMock).has("Authorization")).toBe(false);
  });

  it("sends refresh without a bearer token", async () => {
    setAccessToken("stale-token");
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(authResponse));
    vi.stubGlobal("fetch", fetchMock);

    await request("/api/auth/refresh", { method: "POST", requiresAuth: false });

    expect(requestHeaders(fetchMock).has("Authorization")).toBe(false);
  });

  it("sends a bearer token for authenticated requests", async () => {
    setAccessToken("active-token");
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse({
        id: "2862539e-1b7f-4f3a-af86-cfc4d66fc761",
        email: "person@example.com",
        displayName: "Test Person",
        selectedOrganizationId: null,
        organizations: [],
      })
    );
    vi.stubGlobal("fetch", fetchMock);

    await api.currentUser();

    expect(requestHeaders(fetchMock).get("Authorization")).toBe("Bearer active-token");
  });

  it("serializes a plain JavaScript object exactly once", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ ok: true }));
    vi.stubGlobal("fetch", fetchMock);

    await request("/api/example", {
      method: "POST",
      requiresAuth: false,
      body: { nested: { value: 42 } },
    });

    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(init.body).toBe('{"nested":{"value":42}}');
    expect(requestHeaders(fetchMock).get("Content-Type")).toBe("application/json");
  });

  it("passes FormData through without setting Content-Type", async () => {
    const formData = new FormData();
    formData.set("attachment", new Blob(["evidence"]), "evidence.txt");
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ ok: true }));
    vi.stubGlobal("fetch", fetchMock);

    await request("/api/upload", {
      method: "POST",
      requiresAuth: false,
      body: formData,
    });

    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(init.body).toBe(formData);
    expect(requestHeaders(fetchMock).has("Content-Type")).toBe(false);
  });

  it("returns a clear structured error when the backend is unavailable", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("Failed to fetch")));

    const failure = await api
      .login("person@example.com", "correct-horse-battery")
      .catch((error) => error);

    expect(failure).toBeInstanceOf(ApiError);
    expect(failure).toMatchObject({
      status: 0,
      code: "BACKEND_UNAVAILABLE",
      networkFailure: true,
    });
    expect((failure as Error).message).toContain("control-plane");
  });

  it("returns undefined for an empty 204 response", async () => {
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue(new Response(null, { status: 204 })));

    await expect(
      request<void>("/api/empty", { method: "DELETE", requiresAuth: false })
    ).resolves.toBeUndefined();
  });

  it("preserves structured field validation errors", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        jsonResponse(
          {
            code: "VALIDATION_ERROR",
            message: "Request validation failed",
            status: 400,
            correlationId: "test-correlation",
            validationErrors: [{ field: "email", message: "Enter a valid email address" }],
          },
          400
        )
      )
    );

    const failure = await api
      .signup("bad", "correct-horse-battery", "Test")
      .catch((error) => error);
    expect(failure).toBeInstanceOf(ApiError);
    expect((failure as ApiError).validationErrors).toEqual([
      { field: "email", message: "Enter a valid email address" },
    ]);
  });
});
