"use client";

import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { Button, StatusBadge } from "@/components/ui";

const steps = [
  [
    "Launch a demo incident",
    "Create a deterministic production-like signal.",
    "/dashboard/incidents",
  ],
  [
    "Inspect grouped signals",
    "Review severity, service, and incident context.",
    "/dashboard/incidents",
  ],
  ["Collect grounded evidence", "Start the bounded diagnostic workflow.", "/dashboard/incidents"],
  ["Review AI analysis", "Inspect citations, confidence, and uncertainty.", "/dashboard/incidents"],
  ["Preview policy", "Confirm the Java policy engine's decision.", "/dashboard/runbooks"],
  ["Approve a safe action", "Use an identity-bound, audited approval.", "/dashboard/approvals"],
  ["Resolve and learn", "Generate a printable postmortem.", "/dashboard/incidents"],
] as const;

export default function GettingStartedPage() {
  const client = useQueryClient();
  const incidents = useQuery({
    queryKey: ["incidents", "getting-started"],
    queryFn: () => api.incidents(new URLSearchParams()),
  });
  const launch = useMutation({
    mutationFn: api.launchDemo,
    onSuccess: async () =>
      client.refetchQueries({
        queryKey: ["incidents", "getting-started"],
        exact: true,
        type: "active",
      }),
  });
  const hasIncident = (incidents.data?.items.length ?? 0) > 0;

  return (
    <main className="mx-auto max-w-5xl p-4 sm:p-6 lg:p-8">
      <p className="text-xs font-semibold tracking-widest text-[var(--color-muted-text)] uppercase">
        Guided first run
      </p>
      <div className="mt-2 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight">See the governed workflow</h1>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-[var(--color-secondary-text)]">
            Follow one incident from detection through evidence, policy, approval, remediation, and
            learning. Every action remains simulated and safe in Demo Mode.
          </p>
        </div>
        <Button onClick={() => launch.mutate()} loading={launch.isPending}>
          {hasIncident ? "Launch another demo" : "Launch demo incident"}
        </Button>
      </div>
      <ol className="mt-8 grid gap-3">
        {steps.map(([title, description, href], index) => {
          const complete = hasIncident && index < 2;
          return (
            <li
              key={title}
              className="flex items-center gap-4 rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-4"
            >
              <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-[var(--color-info-bg)] text-sm font-semibold">
                {index + 1}
              </span>
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <h2 className="text-sm font-semibold">{title}</h2>
                  {complete && <StatusBadge variant="success">Ready</StatusBadge>}
                </div>
                <p className="mt-1 text-xs text-[var(--color-secondary-text)]">{description}</p>
              </div>
              <Link href={href} className="text-sm font-medium underline-offset-4 hover:underline">
                Open
              </Link>
            </li>
          );
        })}
      </ol>
    </main>
  );
}
