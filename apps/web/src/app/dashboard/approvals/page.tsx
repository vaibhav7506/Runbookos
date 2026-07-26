"use client";

import Link from "next/link";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { api } from "@/lib/api";
import { StatusBadge } from "@/components/ui";

export default function ApprovalInboxPage() {
  const [status, setStatus] = useState("PENDING");
  const result = useQuery({
    queryKey: ["approvals", status],
    queryFn: () => api.approvals(status || undefined),
  });
  return (
    <div className="mx-auto max-w-6xl p-4 sm:p-6 lg:p-8">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-xs font-medium tracking-wide text-[var(--color-muted-text)] uppercase">
            Human control point
          </p>
          <h1 className="mt-2 text-3xl font-semibold">Approval inbox</h1>
          <p className="mt-2 text-sm text-[var(--color-secondary-text)]">
            Review risk, policy reasoning, expiry, and prior decisions before responding.
          </p>
        </div>
        <label className="grid gap-1 text-xs font-medium">
          Status
          <select
            value={status}
            onChange={(event) => setStatus(event.target.value)}
            className="rounded-lg border border-[var(--color-border)] bg-transparent px-3 py-2 text-sm"
          >
            <option value="">All</option>
            <option>PENDING</option>
            <option>APPROVED</option>
            <option>DENIED</option>
            <option>EXPIRED</option>
            <option>CANCELLED</option>
          </select>
        </label>
      </header>
      <div className="mt-8 space-y-3">
        {result.data?.map((approval) => (
          <Link
            key={approval.id}
            href={`/dashboard/approvals/${approval.id}`}
            className="grid gap-3 rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] p-5 transition-colors hover:bg-[var(--color-hover)] sm:grid-cols-[1fr_auto]"
          >
            <div>
              <div className="flex flex-wrap items-center gap-2">
                <h2 className="font-semibold">{approval.actionName}</h2>
                <StatusBadge
                  variant={approval.riskClassification === "HIGH_RISK" ? "critical" : "warning"}
                >
                  {approval.riskClassification}
                </StatusBadge>
              </div>
              <p className="mt-2 text-sm text-[var(--color-secondary-text)]">
                {approval.policyReason}
              </p>
              <p className="mt-3 text-xs text-[var(--color-muted-text)]">
                {approval.decisions.length}/{approval.requiredApprovals} approvals · expires{" "}
                {new Date(approval.expiresAt).toLocaleString()}
              </p>
            </div>
            <StatusBadge
              variant={
                approval.status === "APPROVED"
                  ? "success"
                  : approval.status === "PENDING"
                    ? "warning"
                    : "neutral"
              }
            >
              {approval.status}
            </StatusBadge>
          </Link>
        ))}
      </div>
      {!result.isLoading && result.data?.length === 0 && (
        <p className="mt-12 text-center text-sm text-[var(--color-muted-text)]">
          No approvals match this filter.
        </p>
      )}
    </div>
  );
}
