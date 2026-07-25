import { Card, CardHeader, StatusBadge, EmptyState } from "@/components/ui";

export default function DashboardOverview() {
  return (
    <div className="mx-auto max-w-6xl">
      <div className="mb-6">
        <h1 className="text-2xl font-semibold text-[var(--color-primary-text)]">Overview</h1>
        <p className="mt-1 text-sm text-[var(--color-secondary-text)]">
          Operational health and recent activity
        </p>
      </div>

      {/* Status cards */}
      <div className="mb-8 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <MetricCard label="Active Incidents" value="0" status="success" />
        <MetricCard label="Awaiting Approval" value="0" status="info" />
        <MetricCard label="Integrations" value="0" subtitle="Not configured" />
        <MetricCard label="System Health" value="Healthy" status="success" />
      </div>

      {/* Recent activity */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader title="Recent Incidents" />
          <EmptyState
            title="No incidents yet"
            description="Incidents will appear here when signals are received or a demo scenario is triggered."
            icon={
              <svg
                width="32"
                height="32"
                viewBox="0 0 32 32"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.5"
                strokeLinecap="round"
                aria-hidden="true"
              >
                <path d="M16 4L28 26H4L16 4z" />
                <path d="M16 13v5M16 21v1" />
              </svg>
            }
          />
        </Card>

        <Card>
          <CardHeader title="Recent Executions" />
          <EmptyState
            title="No executions yet"
            description="Workflow executions will be tracked here once runbooks are configured."
            icon={
              <svg
                width="32"
                height="32"
                viewBox="0 0 32 32"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.5"
                strokeLinecap="round"
                aria-hidden="true"
              >
                <circle cx="16" cy="16" r="12" />
                <path d="M16 10v6l4 3" />
              </svg>
            }
          />
        </Card>
      </div>
    </div>
  );
}

function MetricCard({
  label,
  value,
  subtitle,
  status,
}: {
  label: string;
  value: string;
  subtitle?: string;
  status?: "success" | "warning" | "critical" | "info";
}) {
  return (
    <div className="rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4">
      <p className="mb-1 text-xs text-[var(--color-secondary-text)]">{label}</p>
      <div className="flex items-center gap-2">
        <p className="text-xl font-semibold text-[var(--color-primary-text)]">{value}</p>
        {status && (
          <StatusBadge variant={status} dot>
            {status === "success"
              ? "Healthy"
              : status === "warning"
                ? "Degraded"
                : status === "critical"
                  ? "Critical"
                  : "Active"}
          </StatusBadge>
        )}
      </div>
      {subtitle && <p className="mt-1 text-xs text-[var(--color-muted-text)]">{subtitle}</p>}
    </div>
  );
}
