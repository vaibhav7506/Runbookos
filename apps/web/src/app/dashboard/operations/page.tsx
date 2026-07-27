"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { Button, EmptyState, Skeleton, StatusBadge } from "@/components/ui";

export default function OperationsPage() {
  const client = useQueryClient();
  const overview = useQuery({
    queryKey: ["operations-overview"],
    queryFn: api.operationsOverview,
    refetchInterval: 15_000,
  });
  const deadLetters = useQuery({ queryKey: ["dead-letters"], queryFn: api.deadLetters });
  const redrive = useMutation({
    mutationFn: api.redriveDeadLetter,
    onSuccess: async () => {
      await Promise.all([
        client.invalidateQueries({ queryKey: ["dead-letters"] }),
        client.invalidateQueries({ queryKey: ["operations-overview"] }),
      ]);
    },
  });
  return (
    <main className="mx-auto max-w-6xl space-y-8 p-4 sm:p-6 lg:p-8">
      <header>
        <p className="text-xs font-semibold tracking-[0.14em] text-[var(--color-muted-text)] uppercase">
          Reliability
        </p>
        <h1 className="mt-2 text-3xl font-semibold tracking-tight">Operational health</h1>
        <p className="mt-2 text-sm text-[var(--color-secondary-text)]">
          Protected platform health, delivery pressure, and safe recovery controls.
        </p>
      </header>
      {overview.isLoading ? (
        <Skeleton className="h-40" />
      ) : overview.isError || !overview.data ? (
        <EmptyState
          title="Health unavailable"
          description="The operations endpoint could not be reached."
          action={<Button onClick={() => overview.refetch()}>Retry</Button>}
        />
      ) : (
        <section className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <HealthCard
            label="Platform"
            value={overview.data.systemStatus}
            healthy={overview.data.systemStatus === "OPERATIONAL"}
          />
          <HealthCard
            label="Active workflows"
            value={String(overview.data.activeExecutions)}
            healthy
          />
          <HealthCard
            label="Dead letters"
            value={String(overview.data.deadLetters)}
            healthy={overview.data.deadLetters === 0}
          />
          <HealthCard
            label="Integration warnings"
            value={String(overview.data.unhealthyIntegrations)}
            healthy={overview.data.unhealthyIntegrations === 0}
          />
          <HealthCard
            label="Pending approvals"
            value={String(overview.data.pendingApprovals)}
            healthy
          />
          <HealthCard label="Uptime" value={overview.data.uptime} healthy />
        </section>
      )}
      <section className="rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-5 sm:p-6">
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2 className="text-lg font-semibold">Dead-letter queue</h2>
            <p className="mt-1 text-sm text-[var(--color-secondary-text)]">
              Only owners and administrators can redrive failed workflow events.
            </p>
          </div>
          <StatusBadge variant={deadLetters.data?.length ? "warning" : "success"}>
            {deadLetters.data?.length ?? 0} events
          </StatusBadge>
        </div>
        <div className="mt-6">
          {deadLetters.isLoading ? (
            <Skeleton className="h-28" />
          ) : deadLetters.isError ? (
            <p role="alert" className="text-sm text-[var(--color-critical)]">
              Dead letters could not be loaded.
            </p>
          ) : deadLetters.data?.length ? (
            <ul className="divide-y divide-[var(--color-border)]">
              {deadLetters.data.map((event) => (
                <li
                  key={event.id}
                  className="flex flex-col gap-4 py-4 first:pt-0 sm:flex-row sm:items-center sm:justify-between"
                >
                  <div>
                    <p className="text-sm font-medium">{event.eventType}</p>
                    <p className="mt-1 text-xs text-[var(--color-muted-text)]">
                      {event.aggregateType} · {event.attempts} attempts · {event.lastError}
                    </p>
                  </div>
                  <Button
                    size="sm"
                    variant="secondary"
                    loading={redrive.isPending}
                    onClick={() => redrive.mutate(event.id)}
                  >
                    Redrive safely
                  </Button>
                </li>
              ))}
            </ul>
          ) : (
            <EmptyState
              title="Queue is clear"
              description="No workflow event has exhausted delivery retries."
            />
          )}
        </div>
      </section>
    </main>
  );
}
function HealthCard({ label, value, healthy }: { label: string; value: string; healthy: boolean }) {
  return (
    <article className="rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-5">
      <div className="flex items-center justify-between">
        <p className="text-xs text-[var(--color-muted-text)]">{label}</p>
        <span
          className={`h-2 w-2 rounded-full ${healthy ? "bg-[var(--color-success)]" : "bg-[var(--color-warning)]"}`}
        />
      </div>
      <p className="mt-4 text-xl font-semibold">{value}</p>
    </article>
  );
}
