import { beforeEach, describe, expect, it, vi } from "vitest";
import { api, ApiError, setAccessToken } from "./api";

describe("API client error handling", () => {
  beforeEach(() => {
    setAccessToken(null);
    vi.restoreAllMocks();
  });

  it("preserves structured field validation errors", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(
          JSON.stringify({
            code: "VALIDATION_ERROR",
            message: "Request validation failed",
            status: 400,
            correlationId: "test-correlation",
            validationErrors: [{ field: "email", message: "Enter a valid email address" }],
          }),
          { status: 400, headers: { "Content-Type": "application/json" } }
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
