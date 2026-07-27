"use client";

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { Button, EmptyState, Skeleton, StatusBadge } from "@/components/ui";

export default function AuditPage() {
  const [page, setPage] = useState(0);
  const events = useQuery({
    queryKey: ["audit", page, 25],
    queryFn: () => api.auditEvents(page, 25),
  });
  return (
    <main className="mx-auto max-w-7xl space-y-7 p-4 sm:p-6 lg:p-8">
      <header>
        <p className="text-xs font-semibold tracking-[0.14em] text-[var(--color-muted-text)] uppercase">
          Integrity protected
        </p>
        <h1 className="mt-2 text-3xl font-semibold tracking-tight">Audit log</h1>
        <p className="mt-2 text-sm text-[var(--color-secondary-text)]">
          Append-only decisions and security events with correlation and hash-chain references.
        </p>
      </header>
      {events.isLoading ? (
        <div className="space-y-3">
          <Skeleton className="h-20" />
          <Skeleton className="h-20" />
        </div>
      ) : events.isError ? (
        <EmptyState
          title="Audit log unavailable"
          description="Your role may not permit audit access, or the service is unavailable."
          action={<Button onClick={() => events.refetch()}>Retry</Button>}
        />
      ) : events.data?.items.length ? (
        <>
          <div className="overflow-hidden rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)]">
            <ul className="divide-y divide-[var(--color-border)]">
              {events.data.items.map((event) => (
                <li
                  key={event.id}
                  className="grid gap-3 p-4 sm:grid-cols-[1fr_auto] sm:items-center sm:p-5"
                >
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="font-medium">{pretty(event.action)}</p>
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
                    <p className="mt-1 text-xs text-[var(--color-muted-text)]">
                      {event.actor} · {event.resourceType}
                      {event.resourceId ? `/${event.resourceId}` : ""}
                    </p>
                    <details className="mt-3 text-xs">
                      <summary className="cursor-pointer text-[var(--color-secondary-text)]">
                        Integrity and correlation
                      </summary>
                      <dl className="mt-2 grid gap-1 font-mono text-[11px] text-[var(--color-muted-text)]">
                        <div>Correlation: {event.correlationId || "not supplied"}</div>
                        <div className="truncate">Hash: {event.integrityHash}</div>
                      </dl>
                    </details>
                  </div>
                  <time
                    className="text-xs text-[var(--color-muted-text)]"
                    dateTime={event.occurredAt}
                  >
                    {new Intl.DateTimeFormat(undefined, {
                      dateStyle: "medium",
                      timeStyle: "medium",
                    }).format(new Date(event.occurredAt))}
                  </time>
                </li>
              ))}
            </ul>
          </div>
          <nav aria-label="Audit pagination" className="flex items-center justify-between">
            <Button
              variant="secondary"
              disabled={page === 0}
              onClick={() => setPage((value) => Math.max(0, value - 1))}
            >
              Previous
            </Button>
            <span className="text-xs text-[var(--color-muted-text)]">
              Page {page + 1} of {Math.max(events.data.totalPages, 1)}
            </span>
            <Button
              variant="secondary"
              disabled={page + 1 >= events.data.totalPages}
              onClick={() => setPage((value) => value + 1)}
            >
              Next
            </Button>
          </nav>
        </>
      ) : (
        <EmptyState
          title="No audit activity"
          description="Governed actions will be recorded here."
        />
      )}
    </main>
  );
}
function pretty(value: string) {
  return value
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/^\w/, (letter) => letter.toUpperCase());
}
