"use client";

import type { StepsRequest } from "@runbookos/api-client";
import { useParams } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useMemo, useState } from "react";
import { api } from "@/lib/api";
import { Button, StatusBadge } from "@/components/ui";

type DraftStep = StepsRequest["steps"][number];

export default function RunbookDetailPage() {
  const { id } = useParams<{ id: string }>();
  const client = useQueryClient();
  const result = useQuery({ queryKey: ["runbook", id], queryFn: () => api.runbook(id) });
  const [draft, setDraft] = useState<DraftStep[] | null>(null);
  const [environment, setEnvironment] = useState("STAGING");
  const steps = useMemo<DraftStep[]>(
    () =>
      draft ??
      result.data?.steps.map((step) => ({
        stepKey: step.stepKey,
        name: step.name,
        description: step.description ?? "",
        sequenceNumber: step.sequenceNumber,
        stepType: step.stepType,
        riskClassification: step.riskClassification,
        requiredRole: step.requiredRole,
        timeoutSeconds: step.timeoutSeconds,
        maxRetries: step.maxRetries,
        rollbackInformation: step.rollbackInformation ?? "",
        allowedEnvironments: step.allowedEnvironments as DraftStep["allowedEnvironments"],
        configuration: step.configuration,
      })) ??
      [],
    [draft, result.data]
  );
  const save = useMutation({
    mutationFn: () => api.replaceRunbookSteps(id, result.data!.selectedVersion.id, { steps }),
    onSuccess: () => {
      setDraft(null);
      client.invalidateQueries({ queryKey: ["runbook", id] });
    },
  });
  const preview = useMutation({
    mutationFn: () => api.previewRunbook(id, result.data!.selectedVersion.id, environment),
  });
  const publish = useMutation({
    mutationFn: () => api.publishRunbook(id, result.data!.selectedVersion.id, environment),
    onSuccess: () => client.invalidateQueries({ queryKey: ["runbook", id] }),
  });
  if (!result.data) return <p className="p-8 text-sm">Loading runbook…</p>;
  const data = result.data;
  const editable = data.selectedVersion.status === "DRAFT";
  return (
    <div className="mx-auto max-w-5xl p-4 sm:p-6 lg:p-8">
      <header className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <StatusBadge variant={editable ? "warning" : "success"}>
              {data.selectedVersion.status}
            </StatusBadge>
            <span className="text-xs text-[var(--color-muted-text)]">
              Version {data.selectedVersion.versionNumber}
            </span>
          </div>
          <h1 className="mt-3 text-3xl font-semibold">{data.runbook.name}</h1>
          <p className="mt-2 text-sm text-[var(--color-secondary-text)]">
            {data.runbook.description}
          </p>
        </div>
        {editable && (
          <Button onClick={() => save.mutate()} loading={save.isPending}>
            Save draft
          </Button>
        )}
      </header>
      <section className="mt-8 rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] p-5">
        <h2 className="text-lg font-semibold">Structured step builder</h2>
        <p className="mt-1 text-sm text-[var(--color-secondary-text)]">
          Steps run top to bottom. Published versions are permanently read-only.
        </p>
        <ol className="mt-6 space-y-4">
          {steps.map((step, index) => (
            <li key={`${step.stepKey}-${index}`} className="grid grid-cols-[32px_1fr] gap-3">
              <span className="flex h-8 w-8 items-center justify-center rounded-full bg-[var(--color-info-bg)] text-sm font-medium">
                {index + 1}
              </span>
              <fieldset
                disabled={!editable}
                className="grid gap-3 rounded-lg border border-[var(--color-border)] p-4"
              >
                <div className="grid gap-3 sm:grid-cols-2">
                  <Field
                    label="Step name"
                    value={step.name}
                    onChange={(value) => update(index, { name: value })}
                  />
                  <Field
                    label="Stable key"
                    value={step.stepKey}
                    onChange={(value) => update(index, { stepKey: value })}
                  />
                  <Select
                    label="Type"
                    value={step.stepType}
                    values={[
                      "HTTP_REQUEST",
                      "N8N_WORKFLOW",
                      "GITHUB_ACTION",
                      "NOTIFICATION",
                      "MANUAL_TASK",
                      "WAIT",
                      "CONDITION",
                      "AI_RECOMMENDATION",
                    ]}
                    onChange={(value) =>
                      update(index, { stepType: value as DraftStep["stepType"] })
                    }
                  />
                  <Select
                    label="Risk"
                    value={step.riskClassification}
                    values={["READ_ONLY", "REVERSIBLE", "HIGH_RISK", "PROHIBITED"]}
                    onChange={(value) =>
                      update(index, {
                        riskClassification: value as DraftStep["riskClassification"],
                      })
                    }
                  />
                </div>
                <Field
                  label="Rollback information"
                  value={step.rollbackInformation}
                  onChange={(value) => update(index, { rollbackInformation: value })}
                />
                <p className="text-xs text-[var(--color-muted-text)]">
                  Role {step.requiredRole} · timeout {step.timeoutSeconds}s · retries{" "}
                  {step.maxRetries} · {step.allowedEnvironments.join(", ")}
                </p>
              </fieldset>
            </li>
          ))}
        </ol>
        {editable && (
          <Button
            variant="secondary"
            className="mt-4"
            onClick={() =>
              setDraft([
                ...steps,
                {
                  stepKey: `step-${steps.length + 1}`,
                  name: "New diagnostic step",
                  description: "",
                  sequenceNumber: steps.length + 1,
                  stepType: "MANUAL_TASK",
                  riskClassification: "READ_ONLY",
                  requiredRole: "RESPONDER",
                  timeoutSeconds: 300,
                  maxRetries: 0,
                  rollbackInformation: "No state change.",
                  allowedEnvironments: ["DEVELOPMENT", "STAGING", "PRODUCTION"],
                  configuration: {},
                },
              ])
            }
          >
            Add step
          </Button>
        )}
      </section>
      {editable && (
        <section className="mt-6 rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] p-5">
          <h2 className="text-lg font-semibold">Policy preview</h2>
          <div className="mt-4 flex flex-wrap gap-2">
            <select
              value={environment}
              onChange={(event) => setEnvironment(event.target.value)}
              className="rounded-lg border border-[var(--color-border)] bg-transparent px-3 py-2 text-sm"
            >
              <option>DEVELOPMENT</option>
              <option>STAGING</option>
              <option>PRODUCTION</option>
            </select>
            <Button variant="secondary" onClick={() => preview.mutate()}>
              Preview
            </Button>
            <Button onClick={() => publish.mutate()} loading={publish.isPending}>
              Publish immutable version
            </Button>
          </div>
          {Array.isArray(preview.data) && (
            <ul className="mt-4 space-y-2 text-sm">
              {preview.data.map((item) => (
                <li key={item.stepKey} className="rounded-lg bg-[var(--color-info-bg)] p-3">
                  <strong>{item.stepKey}</strong> · {item.policy.decision}
                  <p className="text-xs text-[var(--color-secondary-text)]">{item.policy.reason}</p>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}
    </div>
  );
  function update(index: number, patch: Partial<DraftStep>) {
    setDraft(steps.map((step, current) => (current === index ? { ...step, ...patch } : step)));
  }
}

function Field({
  label,
  value,
  onChange,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
}) {
  return (
    <label className="grid gap-1 text-xs font-medium">
      {label}
      <input
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="rounded-lg border border-[var(--color-border)] bg-transparent px-3 py-2 text-sm"
      />
    </label>
  );
}
function Select({
  label,
  value,
  values,
  onChange,
}: {
  label: string;
  value: string;
  values: string[];
  onChange: (value: string) => void;
}) {
  return (
    <label className="grid gap-1 text-xs font-medium">
      {label}
      <select
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="rounded-lg border border-[var(--color-border)] bg-transparent px-3 py-2 text-sm"
      >
        {values.map((item) => (
          <option key={item}>{item}</option>
        ))}
      </select>
    </label>
  );
}
