"use client";

import type { IntegrationSetupRequest, IntegrationView } from "@runbookos/api-client";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useMemo, useState } from "react";
import { ApiError, api } from "@/lib/api";
import { Button, EmptyState, Skeleton, StatusBadge } from "@/components/ui";

const connectors: Array<{
  kind: IntegrationSetupRequest["kind"];
  label: string;
  description: string;
}> = [
  {
    kind: "GITHUB",
    label: "GitHub",
    description: "Repositories, commits, deployments, pull requests, and issues.",
  },
  {
    kind: "SENTRY",
    label: "Sentry-compatible",
    description: "Signed error and alert webhook ingestion.",
  },
  {
    kind: "SLACK",
    label: "Slack",
    description: "Incident, approval, status, and resolution notifications.",
  },
  {
    kind: "EMAIL",
    label: "Email",
    description: "Approval notices, incident digests, and resolution summaries.",
  },
  {
    kind: "JIRA",
    label: "Jira-compatible",
    description: "Approval-gated issue creation and comments.",
  },
  {
    kind: "CUSTOM_WEBHOOK",
    label: "Signed webhook",
    description: "HMAC-verified inbound and outbound automation.",
  },
  {
    kind: "HTTP_HEALTHCHECK",
    label: "HTTP health check",
    description: "Allowlisted public GET and HEAD diagnostics.",
  },
];

const fieldClass =
  "min-h-10 rounded-xl border border-[var(--color-border)] bg-transparent px-3 py-2 outline-none transition-colors focus:border-[var(--color-accent)] focus:ring-2 focus:ring-[var(--color-accent)]/20 disabled:cursor-not-allowed disabled:opacity-60";

export default function IntegrationsPage() {
  const queryClient = useQueryClient();
  const integrations = useQuery({ queryKey: ["integrations"], queryFn: api.integrations });
  const [kind, setKind] = useState<IntegrationSetupRequest["kind"]>("GITHUB");
  const [name, setName] = useState("");
  const [environment, setEnvironment] =
    useState<IntegrationSetupRequest["environment"]>("DEVELOPMENT");
  const [scope, setScope] = useState("");
  const [secret, setSecret] = useState("");
  const [demo, setDemo] = useState(true);
  const dirty = Boolean(name || scope || secret);
  useEffect(() => {
    const warn = (event: BeforeUnloadEvent) => {
      if (!dirty) return;
      event.preventDefault();
    };
    window.addEventListener("beforeunload", warn);
    return () => window.removeEventListener("beforeunload", warn);
  }, [dirty]);
  const selected = useMemo(() => connectors.find((item) => item.kind === kind), [kind]);

  const create = useMutation({
    mutationFn: () =>
      api.createIntegration({
        kind,
        name,
        environment,
        config: {
          demo,
          scope,
          ...(kind === "GITHUB" ? { repository: scope } : {}),
        },
        secret,
      }),
    onSuccess: async (value) => {
      setName("");
      setScope("");
      setSecret("");
      await queryClient.invalidateQueries({ queryKey: ["integrations"] });
      if (demo || value.kind === "GITHUB") validate.mutate(value.id);
    },
  });
  const validate = useMutation({
    mutationFn: api.validateIntegration,
    onSettled: () => queryClient.invalidateQueries({ queryKey: ["integrations"] }),
  });
  const disconnect = useMutation({
    mutationFn: api.disconnectIntegration,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["integrations"] }),
  });

  const error = (create.error ?? validate.error ?? disconnect.error) as ApiError | null;

  return (
    <main className="mx-auto max-w-6xl space-y-8 p-4 sm:p-6 lg:p-8">
      <header>
        <p className="text-xs font-semibold tracking-[0.14em] text-[var(--color-accent)] uppercase">
          Connections
        </p>
        <h1 className="mt-2 text-3xl font-semibold tracking-tight">Integrations</h1>
        <p className="mt-2 max-w-2xl text-sm text-[var(--color-secondary-text)]">
          Connect production systems or use transparent demo adapters. Saved secrets are encrypted
          and are never displayed again.
        </p>
      </header>

      <section className="rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-5 sm:p-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2 className="text-lg font-semibold">Add a connection</h2>
            <p className="mt-1 text-sm text-[var(--color-secondary-text)]">
              {selected?.description}
            </p>
          </div>
          <span className="rounded-full bg-[var(--color-hover)] px-3 py-1 text-xs font-medium">
            Step 1 of 3
          </span>
        </div>
        <form
          className="mt-6 grid gap-4 sm:grid-cols-2"
          onSubmit={(event) => {
            event.preventDefault();
            create.mutate();
          }}
        >
          <Field label="Connector">
            <select
              value={kind}
              onChange={(event) => setKind(event.target.value as IntegrationSetupRequest["kind"])}
              className={fieldClass}
            >
              {connectors.map((connector) => (
                <option key={connector.kind} value={connector.kind}>
                  {connector.label}
                </option>
              ))}
            </select>
          </Field>
          <Field label="Environment">
            <select
              value={environment}
              onChange={(event) =>
                setEnvironment(event.target.value as IntegrationSetupRequest["environment"])
              }
              className={fieldClass}
            >
              <option value="DEVELOPMENT">Development</option>
              <option value="STAGING">Staging</option>
              <option value="PRODUCTION">Production</option>
            </select>
          </Field>
          <Field label="Connection name">
            <input
              className={fieldClass}
              value={name}
              onChange={(event) => setName(event.target.value)}
              required
              maxLength={120}
              placeholder={`${selected?.label ?? "Integration"} — ${environment.toLowerCase()}`}
            />
          </Field>
          <Field label={kind === "GITHUB" ? "Repository scope (owner/name)" : "Scope"}>
            <input
              className={fieldClass}
              value={scope}
              onChange={(event) => setScope(event.target.value)}
              required
              placeholder={kind === "GITHUB" ? "acme/checkout-api" : "Incident response"}
            />
          </Field>
          <label className="flex items-center gap-3 rounded-xl border border-[var(--color-border)] p-4 text-sm">
            <input
              type="checkbox"
              checked={demo}
              onChange={(event) => setDemo(event.target.checked)}
            />
            <span>
              <span className="block font-medium">Use demo adapter</span>
              <span className="text-[var(--color-muted-text)]">
                No external request will be made.
              </span>
            </span>
          </label>
          <Field label="Credential">
            <input
              className={fieldClass}
              type="password"
              value={secret}
              onChange={(event) => setSecret(event.target.value)}
              required={!demo}
              disabled={demo}
              autoComplete="new-password"
              placeholder={demo ? "Not required in Demo Mode" : "Stored once, never returned"}
            />
          </Field>
          <div className="rounded-xl bg-[var(--color-hover)] p-4 text-sm sm:col-span-2">
            <strong>Permissions.</strong> {permissionFor(kind)}
          </div>
          {error && (
            <p role="alert" className="text-sm text-[var(--color-danger)] sm:col-span-2">
              {error.message}
              {error.correlationId ? ` · Reference ${error.correlationId}` : ""}
            </p>
          )}
          <div className="flex items-center gap-3 sm:col-span-2">
            <Button type="submit" loading={create.isPending}>
              Save and test connection
            </Button>
            <span className="text-xs text-[var(--color-muted-text)]">
              Validation never returns the credential.
            </span>
          </div>
        </form>
      </section>

      <section>
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold">Connection health</h2>
          <span className="text-xs text-[var(--color-muted-text)]">
            {integrations.data?.length ?? 0} configured
          </span>
        </div>
        {integrations.isLoading ? (
          <div className="grid gap-4 md:grid-cols-2">
            <Skeleton className="h-44" />
            <Skeleton className="h-44" />
          </div>
        ) : integrations.isError ? (
          <EmptyState
            title="Connections unavailable"
            description="Check the control plane and try again."
          />
        ) : integrations.data?.length ? (
          <div className="grid gap-4 md:grid-cols-2">
            {integrations.data.map((item) => (
              <IntegrationCard
                key={item.id}
                item={item}
                validating={validate.isPending}
                disconnecting={disconnect.isPending}
                onValidate={() => validate.mutate(item.id)}
                onDisconnect={() => {
                  if (
                    window.confirm(
                      `Disconnect ${item.name}? Its stored credential will be revoked.`
                    )
                  ) {
                    disconnect.mutate(item.id);
                  }
                }}
              />
            ))}
          </div>
        ) : (
          <EmptyState
            title="No integrations yet"
            description="Add a transparent demo adapter or validate a live GitHub connection."
          />
        )}
      </section>
    </main>
  );
}

function IntegrationCard({
  item,
  validating,
  disconnecting,
  onValidate,
  onDisconnect,
}: {
  item: IntegrationView;
  validating: boolean;
  disconnecting: boolean;
  onValidate: () => void;
  onDisconnect: () => void;
}) {
  const variant =
    item.status === "CONNECTED"
      ? "success"
      : item.status === "DEGRADED"
        ? "warning"
        : item.status === "ERROR"
          ? "critical"
          : "neutral";
  return (
    <article className="rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-5">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-xs font-medium text-[var(--color-muted-text)]">{item.kind}</p>
          <h3 className="mt-1 font-semibold">{item.name}</h3>
        </div>
        <StatusBadge variant={variant}>{item.status}</StatusBadge>
      </div>
      <dl className="mt-5 grid grid-cols-2 gap-4 text-xs">
        <div>
          <dt className="text-[var(--color-muted-text)]">Environment</dt>
          <dd className="mt-1 font-medium">{item.environment}</dd>
        </div>
        <div>
          <dt className="text-[var(--color-muted-text)]">Last successful use</dt>
          <dd className="mt-1 font-medium">
            {item.lastSuccessAt ? new Date(item.lastSuccessAt).toLocaleString() : "Not yet"}
          </dd>
        </div>
      </dl>
      {item.credentials.length > 0 && (
        <p className="mt-4 text-xs text-[var(--color-muted-text)]">
          Credential fingerprint {item.credentials[0].fingerprint}
        </p>
      )}
      {item.lastErrorMessage && (
        <p className="mt-3 text-xs text-[var(--color-danger)]">{item.lastErrorMessage}</p>
      )}
      <div className="mt-5 flex gap-2">
        <Button size="sm" variant="secondary" loading={validating} onClick={onValidate}>
          Test
        </Button>
        {item.status !== "DISCONNECTED" && (
          <Button size="sm" variant="ghost" loading={disconnecting} onClick={onDisconnect}>
            Disconnect
          </Button>
        )}
      </div>
    </article>
  );
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <label className="grid gap-2 text-sm font-medium">
      {label}
      {children}
    </label>
  );
}

function permissionFor(kind: IntegrationSetupRequest["kind"]) {
  switch (kind) {
    case "GITHUB":
      return "Metadata and Contents read. Issues write is only used after approval.";
    case "SLACK":
      return "Message delivery and signed interactive callbacks.";
    case "EMAIL":
      return "Send-only access; no mailbox read permission.";
    case "JIRA":
      return "Project browse and issue create.";
    case "SENTRY":
      return "Signed webhook ingestion; no account access.";
    case "CUSTOM_WEBHOOK":
      return "HMAC signing with explicit destination allowlisting.";
    case "HTTP_HEALTHCHECK":
      return "Public allowlisted GET or HEAD targets only.";
  }
}
