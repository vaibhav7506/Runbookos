"use client";

import type { DecisionRequest } from "@runbookos/api-client";
import { useParams } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { api } from "@/lib/api";
import { Button, StatusBadge } from "@/components/ui";

export default function ApprovalDetailPage() {
  const { id } = useParams<{ id: string }>();
  const client = useQueryClient();
  const [phrase, setPhrase] = useState("");
  const [reason, setReason] = useState("");
  const result = useQuery({ queryKey: ["approval", id], queryFn: () => api.approval(id) });
  const decide = useMutation({
    mutationFn: (decision: DecisionRequest["decision"]) =>
      api.decideApproval(id, { decision, reason, confirmationPhrase: phrase }),
    onSuccess: () => client.invalidateQueries({ queryKey: ["approval", id] }),
  });
  if (!result.data) return <p className="p-8 text-sm">Loading approval…</p>;
  const approval = result.data;
  const pending = approval.status === "PENDING";
  return (
    <div className="mx-auto max-w-4xl p-4 sm:p-6 lg:p-8">
      <header>
        <div className="flex flex-wrap gap-2">
          <StatusBadge
            variant={approval.riskClassification === "HIGH_RISK" ? "critical" : "warning"}
          >
            {approval.riskClassification}
          </StatusBadge>
          <StatusBadge variant={pending ? "warning" : "neutral"}>{approval.status}</StatusBadge>
        </div>
        <h1 className="mt-3 text-3xl font-semibold">{approval.actionName}</h1>
        <p className="mt-2 text-sm text-[var(--color-secondary-text)]">{approval.policyReason}</p>
      </header>
      <section className="mt-8 rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] p-5">
        <h2 className="font-semibold">Decision context</h2>
        <dl className="mt-4 grid gap-4 text-sm sm:grid-cols-2">
          <Item label="Environment" value={approval.environment} />
          <Item label="Policy decision" value={approval.policyDecision} />
          <Item label="Required approvals" value={`${approval.requiredApprovals}`} />
          <Item label="Expires" value={new Date(approval.expiresAt).toLocaleString()} />
          <Item label="Step key" value={approval.stepKey} />
          <Item label="Requested by" value={approval.requestedBy} />
        </dl>
      </section>
      <section className="mt-6 rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] p-5">
        <h2 className="font-semibold">Human decisions</h2>
        <ol className="mt-4 space-y-2">
          {approval.decisions.map((item) => (
            <li
              key={item.approverUserId}
              className="rounded-lg bg-[var(--color-info-bg)] p-3 text-sm"
            >
              <strong>{item.decision}</strong> by {item.approverUserId}
              <p className="text-xs text-[var(--color-secondary-text)]">
                {item.reason || "No reason supplied"}
              </p>
            </li>
          ))}
        </ol>
        {pending && (
          <form
            onSubmit={(event) => event.preventDefault()}
            className="mt-6 grid gap-4 border-t border-[var(--color-border)] pt-5"
          >
            <label className="grid gap-2 text-sm font-medium">
              Decision reason
              <textarea
                value={reason}
                onChange={(event) => setReason(event.target.value)}
                maxLength={1000}
                className="min-h-24 rounded-lg border border-[var(--color-border)] bg-transparent p-3"
              />
            </label>
            {approval.confirmationPhrase && (
              <label className="grid gap-2 text-sm font-medium">
                Type <code>{approval.confirmationPhrase}</code>
                <input
                  value={phrase}
                  onChange={(event) => setPhrase(event.target.value)}
                  autoComplete="off"
                  className="rounded-lg border border-[var(--color-border)] bg-transparent px-3 py-2"
                />
              </label>
            )}
            <div className="flex gap-2">
              <Button
                type="button"
                onClick={() => decide.mutate("APPROVE")}
                loading={decide.isPending}
              >
                Approve
              </Button>
              <Button
                type="button"
                variant="danger"
                onClick={() => decide.mutate("DENY")}
                loading={decide.isPending}
              >
                Deny
              </Button>
            </div>
            {decide.error && (
              <p role="alert" className="text-sm text-[var(--color-critical)]">
                {decide.error.message}
              </p>
            )}
          </form>
        )}
      </section>
    </div>
  );
}
function Item({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-xs text-[var(--color-muted-text)]">{label}</dt>
      <dd className="mt-1 break-all">{value}</dd>
    </div>
  );
}
