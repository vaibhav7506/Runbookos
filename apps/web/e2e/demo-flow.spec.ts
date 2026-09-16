import { expect, test, type Page } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

const apiBase = process.env.E2E_API_URL ?? "http://127.0.0.1:8080";

async function authorized(
  page: Page,
  path: string,
  options: { method?: string; data?: unknown; headers?: Record<string, string> } = {}
) {
  const token = await page.evaluate(() => sessionStorage.getItem("runbookos_access"));
  return page.request.fetch(`${apiBase}${path}`, {
    method: options.method ?? "GET",
    data: options.data,
    headers: { Authorization: `Bearer ${token}`, ...options.headers },
  });
}

test("onboards a user and completes the governed demo lifecycle", async ({ page }, testInfo) => {
  test.setTimeout(120_000);
  const browserErrors: string[] = [];
  const emptyResponses = new Set<string>();
  page.on("pageerror", (error) => browserErrors.push(error.message));
  page.on("console", (message) => {
    if (message.type() === "error") {
      browserErrors.push(`${message.text()} [${message.location().url}]`);
    }
  });
  page.on("response", (response) => {
    if (response.status() === 204) emptyResponses.add(response.url());
    if (response.status() >= 400) browserErrors.push(`HTTP ${response.status()} ${response.url()}`);
  });
  page.on("requestfailed", (request) => {
    if (request.url().includes("/api/")) {
      browserErrors.push(`${request.method()} ${request.url()}: ${request.failure()?.errorText}`);
    }
  });
  const identity = `${Date.now()}-${testInfo.project.name}`;
  await page.goto("/register");
  await page.getByRole("textbox", { name: "Name" }).fill("Demo Responder");
  await page.getByRole("textbox", { name: "Email address" }).fill(`demo-${identity}@example.com`);
  await page.getByRole("textbox", { name: "Password" }).fill("DemoPassword42!");
  const signupResponsePromise = page.waitForResponse(
    (response) => response.url().endsWith("/api/auth/signup") && response.request().method() === "POST"
  );
  await page.getByRole("button", { name: "Continue" }).click();
  const signupResponse = await signupResponsePromise;
  expect(signupResponse.ok(), `Signup returned HTTP ${signupResponse.status()}`).toBeTruthy();
  await expect(page.getByRole("heading", { name: "Create your response workspace" })).toBeVisible();

  await page.getByRole("textbox", { name: "Organization name" }).fill(`Demo ${identity}`);
  await page.getByRole("button", { name: "Create workspace" }).click();
  await expect(page.getByRole("heading", { name: "See the governed workflow" })).toBeVisible();
  const launchResponsePromise = page.waitForResponse(
    (response) =>
      response.url().endsWith("/api/incidents/demo") && response.request().method() === "POST"
  );
  await page.getByRole("button", { name: /Launch demo incident/ }).click();
  const launchResponse = await launchResponsePromise;
  expect(
    launchResponse.ok(),
    `Demo launch returned HTTP ${launchResponse.status()}: ${await launchResponse.text()}`
  ).toBeTruthy();

  const incidents = await authorized(page, "/api/incidents");
  expect(incidents.ok(), `Incident list returned HTTP ${incidents.status()}`).toBeTruthy();
  const incidentPage = await incidents.json();
  const incident = incidentPage.items[0];
  expect(incident.title).toBe("Checkout error rate increased");

  const analysis = await authorized(page, `/api/incidents/${incident.id}/analyses`, {
    method: "POST",
  });
  expect(analysis.ok()).toBeTruthy();
  for (const status of [
    "TRIAGED",
    "INVESTIGATING",
    "AWAITING_APPROVAL",
    "MITIGATING",
    "MONITORING",
    "RESOLVED",
  ]) {
    const transition = await authorized(page, `/api/incidents/${incident.id}/transitions`, {
      method: "POST",
      data: { status },
    });
    expect(transition.ok()).toBeTruthy();
  }

  await page.goto(`/dashboard/incidents/${incident.id}`);
  await expect(page.getByRole("heading", { name: "Checkout error rate increased" })).toBeVisible();
  await expect(page.getByRole("heading", { name: "AI analysis", level: 2 })).toBeVisible();
  await expect(page.getByText("Resolution & postmortem")).toBeVisible();
  await page.getByRole("button", { name: "Generate postmortem" }).click();
  await expect(page.getByText("Learning artifact", { exact: true })).toBeVisible();
  const unexpectedErrors = browserErrors.filter(
    (error) =>
      ![...emptyResponses].some(
        (url) => error === `GET ${url}: net::ERR_ABORTED` && url.endsWith("/postmortem")
      )
  );
  expect(unexpectedErrors, "The browser console and API requests should have no errors").toEqual([]);
});

test("registration and landing page have no serious accessibility violations", async ({ page }) => {
  await page.goto("/register");
  const results = await new AxeBuilder({ page })
    .withTags(["wcag2a", "wcag2aa", "wcag21aa", "wcag22aa"])
    .analyze();
  expect(
    results.violations.filter((violation) =>
      ["critical", "serious"].includes(violation.impact ?? "")
    )
  ).toEqual([]);
});
