"use client";

import type { PolicyView, RuleRequest } from "@runbookos/api-client";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { api } from "@/lib/api";
import { Button, StatusBadge } from "@/components/ui";

export default function PolicyEditorPage() {
  const result = useQuery({ queryKey: ["policies"], queryFn: api.policies });
  return (
    <div className="mx-auto max-w-6xl p-4 sm:p-6 lg:p-8">
      <header>
        <p className="text-xs font-medium tracking-wide text-[var(--color-muted-text)] uppercase">
          Java authorization engine
        </p>
        <h1 className="mt-2 text-3xl font-semibold">Environment policies</h1>
        <p className="mt-2 max-w-3xl text-sm text-[var(--color-secondary-text)]">
          These rules are authoritative. Prohibited actions remain denied and high-risk rules cannot
          be weakened below two-person approval.
        </p>
      </header>
      <div className="mt-8 space-y-6">
        {result.data?.map((policy) => (
          <PolicyCard key={policy.id} policy={policy} />
        ))}
      </div>
    </div>
  );
}

function PolicyCard({ policy }: { policy: PolicyView }) {
  return (
    <section className="rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] p-5">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="font-semibold">{policy.name}</h2>
          <p className="text-xs text-[var(--color-muted-text)]">{policy.environment}</p>
        </div>
        <StatusBadge variant={policy.enabled ? "success" : "neutral"}>
          {policy.enabled ? "Active" : "Disabled"}
        </StatusBadge>
      </div>
      <div className="mt-5 overflow-x-auto">
        <table className="w-full min-w-[760px] text-left text-sm">
          <thead className="text-xs text-[var(--color-muted-text)]">
            <tr>
              <th className="pb-3">Risk</th>
              <th className="pb-3">Decision</th>
              <th className="pb-3">Approvals</th>
              <th className="pb-3">Confirmation</th>
              <th className="pb-3">Expires</th>
              <th>
                <span className="sr-only">Save</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {policy.rules.map((rule) => (
              <RuleRow key={rule.risk} policyId={policy.id} rule={rule} />
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function RuleRow({ policyId, rule }: { policyId: string; rule: PolicyView["rules"][number] }) {
  const client = useQueryClient();
  const locked = rule.risk === "PROHIBITED";
  const [decision, setDecision] = useState(rule.decision);
  const [approvals, setApprovals] = useState(rule.requiredApprovals);
  const [expiry, setExpiry] = useState(rule.expirationMinutes);
  const update = useMutation({
    mutationFn: () => {
      const value: RuleRequest = {
        decision,
        requiredApprovals: approvals,
        approverRoles: rule.approverRoles,
        typedConfirmationRequired: rule.typedConfirmationRequired,
        expirationMinutes: expiry,
      };
      return api.updatePolicyRule(policyId, rule.risk, value);
    },
    onSuccess: () => client.invalidateQueries({ queryKey: ["policies"] }),
  });
  return (
    <tr className="border-t border-[var(--color-border)]">
      <td className="py-3 font-medium">{rule.risk}</td>
      <td>
        <select
          aria-label={`${rule.risk} decision`}
          disabled={locked || rule.risk === "HIGH_RISK"}
          value={decision}
          onChange={(event) => setDecision(event.target.value as RuleRequest["decision"])}
          className="rounded border border-[var(--color-border)] bg-transparent p-1"
        >
          <option>ALLOW</option>
          <option>REQUIRE_APPROVAL</option>
          <option>REQUIRE_ELEVATED_APPROVAL</option>
          <option>DENY</option>
        </select>
      </td>
      <td>
        <input
          aria-label={`${rule.risk} required approvals`}
          disabled={locked}
          type="number"
          min={rule.risk === "HIGH_RISK" ? 2 : 0}
          max={5}
          value={approvals}
          onChange={(event) => setApprovals(Number(event.target.value))}
          className="w-16 rounded border border-[var(--color-border)] bg-transparent p-1"
        />
      </td>
      <td>{rule.typedConfirmationRequired ? "Typed phrase" : "Not required"}</td>
      <td>
        <label className="flex items-center gap-1">
          <input
            aria-label={`${rule.risk} expiry minutes`}
            disabled={locked}
            type="number"
            min={1}
            max={1440}
            value={expiry}
            onChange={(event) => setExpiry(Number(event.target.value))}
            className="w-20 rounded border border-[var(--color-border)] bg-transparent p-1"
          />{" "}
          min
        </label>
      </td>
      <td className="text-right">
        <Button
          size="sm"
          variant="secondary"
          disabled={locked}
          onClick={() => update.mutate()}
          loading={update.isPending}
        >
          {locked ? "Safety locked" : "Save rule"}
        </Button>
      </td>
    </tr>
  );
}
