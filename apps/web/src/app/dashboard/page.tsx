"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { Button, EmptyState, Skeleton, StatusBadge } from "@/components/ui";

export default function DashboardOverview() {
  const operations = useQuery({
    queryKey: ["operations-overview"],
    queryFn: api.operationsOverview,
    refetchInterval: 30_000,
  });
  const audit = useQuery({
    queryKey: ["audit", 0, 6],
    queryFn: () => api.auditEvents(0, 6),
  });
  const value = operations.data;

  if (operations.isLoading) {
    return (
      <div className="mx-auto max-w-7xl space-y-6">
        <Skeleton className="h-16 w-80" />
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {[1, 2, 3, 4].map((item) => (
            <Skeleton key={item} className="h-32" />
          ))}
        </div>
      </div>
    );
  }

  if (operations.isError || !value) {
    return (
      <EmptyState
        title="Operational overview unavailable"
        description="The control plane did not return health data."
        action={<Button onClick={() => operations.refetch()}>Retry</Button>}
      />
    );
  }

  return (
    <div className="mx-auto max-w-7xl space-y-8">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-xs font-semibold tracking-[0.14em] text-[var(--color-muted-text)] uppercase">
            Command center
          </p>
          <h1 className="mt-2 text-3xl font-semibold tracking-tight">Operational overview</h1>
          <p className="mt-1 text-sm text-[var(--color-secondary-text)]">
            Live health, governed actions, and the work that needs attention.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <StatusBadge variant={value.systemStatus === "OPERATIONAL" ? "success" : "warning"} dot>
            {pretty(value.systemStatus)}
          </StatusBadge>
          <span className="text-xs text-[var(--color-muted-text)]">Uptime {value.uptime}</span>
        </div>
      </header>

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4" aria-label="Key metrics">
        <Metric
          label="Active incidents"
          value={value.activeIncidents}
          href="/dashboard/incidents"
        />
        <Metric
          label="Requiring approval"
          value={value.pendingApprovals}
          href="/dashboard/approvals"
          accent={value.pendingApprovals > 0}
        />
        <Metric label="Active executions" value={value.activeExecutions} />
        <Metric
          label="Integration health"
          value={
            value.unhealthyIntegrations === 0
              ? "Healthy"
              : `${value.unhealthyIntegrations} degraded`
          }
          href="/dashboard/integrations"
          accent={value.unhealthyIntegrations > 0}
        />
      </section>

      <section className="grid gap-6 xl:grid-cols-[1.35fr_1fr]">
        <Panel
          title="Recent executions"
          action={
            <Link
              className="text-xs font-medium underline-offset-4 hover:underline"
              href="/dashboard/incidents"
            >
              View incidents
            </Link>
          }
        >
          {value.recentExecutions.length ? (
            <ul className="divide-y divide-[var(--color-border)]">
              {value.recentExecutions.map((execution) => (
                <li
                  key={execution.id}
                  className="flex items-center justify-between gap-4 py-4 first:pt-0 last:pb-0"
                >
                  <div className="min-w-0">
                    <Link
                      className="truncate text-sm font-medium hover:underline"
                      href={`/dashboard/executions/${execution.id}`}
                    >
                      {pretty(execution.workflowKey)}
                    </Link>
                    <p className="mt-1 text-xs text-[var(--color-muted-text)]">
                      {formatTime(execution.createdAt)}
                    </p>
                  </div>
                  <StatusBadge
                    variant={
                      execution.status === "FAILED"
                        ? "critical"
                        : execution.status === "SUCCEEDED"
                          ? "success"
                          : "info"
                    }
                  >
                    {pretty(execution.status)}
                  </StatusBadge>
                </li>
              ))}
            </ul>
          ) : (
            <EmptyState
              title="No executions yet"
              description="Launch Demo Mode to see the governed workflow."
            />
          )}
        </Panel>

        <div className="grid gap-6">
          <Panel title="AI usage & budget">
            <div className="grid grid-cols-2 gap-5">
              <div>
                <p className="text-2xl font-semibold tabular-nums">
                  {value.aiTokens.toLocaleString()}
                </p>
                <p className="mt-1 text-xs text-[var(--color-muted-text)]">Tokens used</p>
              </div>
              <div>
                <p className="text-2xl font-semibold tabular-nums">
                  ${Number(value.estimatedAiCostUsd).toFixed(4)}
                </p>
                <p className="mt-1 text-xs text-[var(--color-muted-text)]">Estimated cost</p>
              </div>
            </div>
            <div className="mt-5 h-1.5 overflow-hidden rounded-full bg-[var(--color-hover)]">
              <div className="h-full w-[8%] rounded-full bg-[var(--color-control)]" />
            </div>
            <p className="mt-2 text-xs text-[var(--color-muted-text)]">
              Usage remains within configured provider ceilings.
            </p>
          </Panel>
          <Panel title="System health">
            <HealthLine label="Control plane" healthy />
            <HealthLine
              label="Workflow delivery"
              healthy={value.deadLetters === 0}
              detail={value.deadLetters ? `${value.deadLetters} dead letters` : "No dead letters"}
            />
            <HealthLine
              label="Integrations"
              healthy={value.unhealthyIntegrations === 0}
              detail={value.unhealthyIntegrations ? "Attention needed" : "Connected"}
            />
            <Link
              href="/dashboard/operations"
              className="mt-4 inline-block text-xs font-medium underline-offset-4 hover:underline"
            >
              Open operational health
            </Link>
          </Panel>
        </div>
      </section>

      <Panel
        title="Recent audit activity"
        action={
          <Link
            className="text-xs font-medium underline-offset-4 hover:underline"
            href="/dashboard/audit"
          >
            Open audit log
          </Link>
        }
      >
        {audit.isLoading ? (
          <Skeleton className="h-32" />
        ) : audit.isError ? (
          <p role="alert" className="text-sm text-[var(--color-critical)]">
            Audit activity could not be loaded.
          </p>
        ) : audit.data?.items.length ? (
          <div className="grid gap-3 lg:grid-cols-2">
            {audit.data.items.map((event) => (
              <div key={event.id} className="rounded-xl border border-[var(--color-border)] p-4">
                <div className="flex items-center justify-between gap-3">
                  <p className="text-sm font-medium">{pretty(event.action)}</p>
                  <StatusBadge
                    variant={
                      event.outcome === "SUCCESS"
                        ? "success"
                        : event.outcome === "DENIED"
                          ? "warning"
                          : "critical"
                    }
                  >
                    {event.outcome}
                  </StatusBadge>
                </div>
                <p className="mt-2 text-xs text-[var(--color-muted-text)]">
                  {event.resourceType} · {formatTime(event.occurredAt)}
                </p>
              </div>
            ))}
          </div>
        ) : (
          <EmptyState
            title="No audit activity yet"
            description="Security and workflow decisions will appear here."
          />
        )}
      </Panel>
    </div>
  );
}

function Metric({
  label,
  value,
  href,
  accent = false,
}: {
  label: string;
  value: string | number;
  href?: string;
  accent?: boolean;
}) {
  const content = (
    <>
      <p className="text-xs font-medium text-[var(--color-muted-text)]">{label}</p>
      <p className="mt-4 text-3xl font-semibold tracking-tight tabular-nums">{value}</p>
      <p className="mt-3 text-xs text-[var(--color-secondary-text)]">
        {href ? "Open details →" : "Updated live"}
      </p>
    </>
  );
  const classes = `block rounded-2xl border bg-[var(--color-surface)] p-5 transition-colors ${accent ? "border-[var(--color-warning)]" : "border-[var(--color-border)]"} ${href ? "hover:border-[var(--color-border-elevated)]" : ""}`;
  return href ? (
    <Link href={href} className={classes}>
      {content}
    </Link>
  ) : (
    <div className={classes}>{content}</div>
  );
}
function Panel({
  title,
  action,
  children,
}: {
  title: string;
  action?: React.ReactNode;
  children: React.ReactNode;
}) {
  return (
    <section className="rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-5 sm:p-6">
      <div className="mb-5 flex items-center justify-between gap-4">
        <h2 className="text-base font-semibold">{title}</h2>
        {action}
      </div>
      {children}
    </section>
  );
}
function HealthLine({
  label,
  healthy,
  detail,
}: {
  label: string;
  healthy: boolean;
  detail?: string;
}) {
  return (
    <div className="flex items-center justify-between gap-3 border-b border-[var(--color-border)] py-3 last:border-0">
      <div className="flex items-center gap-2">
        <span
          className={`h-2 w-2 rounded-full ${healthy ? "bg-[var(--color-success)]" : "bg-[var(--color-warning)]"}`}
        />
        <span className="text-sm">{label}</span>
      </div>
      <span className="text-xs text-[var(--color-muted-text)]">
        {detail ?? (healthy ? "Healthy" : "Attention")}
      </span>
    </div>
  );
}
function pretty(value: string) {
  return value
    .replaceAll("_", " ")
    .replaceAll("-", " ")
    .toLowerCase()
    .replace(/^\w/, (letter) => letter.toUpperCase());
}
function formatTime(value: string) {
  return new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "short" }).format(
    new Date(value)
  );
}
