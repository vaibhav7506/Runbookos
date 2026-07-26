"use client";
import Link from "next/link";
import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import type { IncidentView } from "@runbookos/api-client";
import { api, ApiError } from "@/lib/api";
import { Button, EmptyState, Skeleton, StatusBadge } from "@/components/ui";

export default function IncidentsPage() {
  const [query, setQuery] = useState("");
  const [severity, setSeverity] = useState("");
  const [status, setStatus] = useState("");
  const params = useMemo(() => {
    const p = new URLSearchParams();
    if (query) p.set("q", query);
    if (severity) p.set("severity", severity);
    if (status) p.set("status", status);
    return p;
  }, [query, severity, status]);
  const result = useQuery({
    queryKey: ["incidents", params.toString()],
    queryFn: () => api.incidents(params),
  });
  const client = useQueryClient();
  const demo = useMutation({
    mutationFn: api.launchDemo,
    onSuccess: () => client.invalidateQueries({ queryKey: ["incidents"] }),
  });
  return (
    <div className="mx-auto max-w-7xl p-4 sm:p-6 lg:p-8">
      <header className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-xs font-medium tracking-widest text-[var(--color-muted-text)] uppercase">
            Operations
          </p>
          <h1 className="mt-2 text-3xl font-semibold">Incident inbox</h1>
          <p className="mt-1 text-sm text-[var(--color-secondary-text)]">
            Signals grouped into actionable incidents.
          </p>
        </div>
        <Button onClick={() => demo.mutate()} loading={demo.isPending}>
          Launch demo incident
        </Button>
      </header>
      <div className="mt-7 grid gap-3 rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] p-3 sm:grid-cols-[1fr_160px_190px]">
        <label>
          <span className="sr-only">Search incidents</span>
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search title or service"
            className="h-10 w-full rounded-lg border border-[var(--color-border)] bg-transparent px-3 text-sm"
          />
        </label>
        <Filter
          label="Severity"
          value={severity}
          onChange={setSeverity}
          options={["SEV1", "SEV2", "SEV3", "SEV4"]}
        />
        <Filter
          label="Status"
          value={status}
          onChange={setStatus}
          options={[
            "DETECTED",
            "TRIAGED",
            "INVESTIGATING",
            "AWAITING_APPROVAL",
            "MITIGATING",
            "MONITORING",
            "RESOLVED",
            "CLOSED",
          ]}
        />
      </div>
      {demo.error && <ErrorMessage error={demo.error} />}
      <section className="mt-5" aria-live="polite">
        {result.isLoading && (
          <div className="space-y-3">
            {[1, 2, 3].map((v) => (
              <Skeleton key={v} className="h-24 w-full" />
            ))}
          </div>
        )}
        {result.error && (
          <div className="rounded-xl border border-[var(--color-critical)]/30 bg-[var(--color-critical-bg)] p-5">
            <ErrorMessage error={result.error} />
            <button onClick={() => result.refetch()} className="mt-3 text-sm font-medium underline">
              Retry
            </button>
          </div>
        )}
        {result.data?.items.length === 0 && (
          <EmptyState
            title="No incidents match"
            description="Adjust the filters or launch the deterministic demo incident."
            action={<Button onClick={() => demo.mutate()}>Launch demo</Button>}
          />
        )}
        {result.data && result.data.items.length > 0 && (
          <>
            <div className="hidden overflow-hidden rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] md:block">
              <table className="w-full text-left text-sm">
                <thead className="border-b border-[var(--color-border)] text-xs text-[var(--color-muted-text)]">
                  <tr>
                    <th className="p-4 font-medium">Incident</th>
                    <th className="p-4 font-medium">Severity</th>
                    <th className="p-4 font-medium">Status</th>
                    <th className="p-4 font-medium">Signals</th>
                    <th className="p-4 font-medium">Last signal</th>
                  </tr>
                </thead>
                <tbody>
                  {result.data.items.map((i) => (
                    <IncidentRow key={i.id} incident={i} />
                  ))}
                </tbody>
              </table>
            </div>
            <div className="space-y-3 md:hidden">
              {result.data.items.map((i) => (
                <IncidentCard key={i.id} incident={i} />
              ))}
            </div>
          </>
        )}
      </section>
    </div>
  );
}
function Filter({
  label,
  value,
  onChange,
  options,
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  options: string[];
}) {
  return (
    <label>
      <span className="sr-only">{label}</span>
      <select
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="h-10 w-full rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)] px-3 text-sm"
      >
        <option value="">All {label.toLowerCase()}</option>
        {options.map((v) => (
          <option key={v}>{v}</option>
        ))}
      </select>
    </label>
  );
}
function IncidentRow({ incident: i }: { incident: IncidentView }) {
  return (
    <tr className="border-b border-[var(--color-border)] last:border-0 hover:bg-[var(--color-hover)]">
      <td className="p-4">
        <Link href={`/dashboard/incidents/${i.id}`} className="font-medium">
          {i.title}
        </Link>
        <p className="mt-1 text-xs text-[var(--color-muted-text)]">{i.affectedService}</p>
      </td>
      <td className="p-4">
        <StatusBadge variant={severityTone(i.severity)}>{i.severity}</StatusBadge>
      </td>
      <td className="p-4 text-[var(--color-secondary-text)]">{pretty(i.status)}</td>
      <td className="p-4">{i.signalCount}</td>
      <td className="p-4 text-[var(--color-secondary-text)]">
        {new Date(i.lastSignalAt).toLocaleString()}
      </td>
    </tr>
  );
}
function IncidentCard({ incident: i }: { incident: IncidentView }) {
  return (
    <Link
      href={`/dashboard/incidents/${i.id}`}
      className="block rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] p-4"
    >
      <div className="flex justify-between gap-3">
        <h2 className="font-medium">{i.title}</h2>
        <StatusBadge variant={severityTone(i.severity)}>{i.severity}</StatusBadge>
      </div>
      <p className="mt-2 text-sm text-[var(--color-secondary-text)]">
        {i.affectedService} · {pretty(i.status)}
      </p>
      <p className="mt-3 text-xs text-[var(--color-muted-text)]">
        {i.signalCount} signal{i.signalCount === 1 ? "" : "s"} ·{" "}
        {new Date(i.lastSignalAt).toLocaleString()}
      </p>
    </Link>
  );
}
function severityTone(value: string): "critical" | "warning" | "info" {
  return value === "SEV1" ? "critical" : value === "SEV2" ? "warning" : "info";
}
function pretty(v: string) {
  return v
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/^\w/, (c) => c.toUpperCase());
}
function ErrorMessage({ error }: { error: Error }) {
  return (
    <p role="alert" className="mt-3 text-sm text-[var(--color-critical)]">
      {error instanceof ApiError
        ? `${error.message} · ${error.correlationId ?? "no correlation ID"}`
        : "The incident service is unavailable."}
    </p>
  );
}
