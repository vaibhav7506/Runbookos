"use client";

import type { AnalysisView } from "@runbookos/api-client";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { Button, StatusBadge } from "@/components/ui";

export function AnalysisPanel({ incidentId }: { incidentId: string }) {
  const client = useQueryClient();
  const result = useQuery({
    queryKey: ["analyses", incidentId],
    queryFn: () => api.analyses(incidentId),
  });
  const analyze = useMutation({
    mutationFn: () => api.analyze(incidentId),
    onSuccess: () => client.invalidateQueries({ queryKey: ["analyses", incidentId] }),
  });
  const latest = result.data?.[0];
  return (
    <section
      aria-labelledby="ai-analysis-title"
      className="rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] p-5"
    >
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <h2 id="ai-analysis-title" className="text-lg font-semibold">
              AI analysis
            </h2>
            <StatusBadge variant="info">Advisory only</StatusBadge>
          </div>
          <p className="mt-1 text-sm text-[var(--color-secondary-text)]">
            Evidence-grounded guidance. Java policy rules—not the model—authorize actions.
          </p>
        </div>
        <Button onClick={() => analyze.mutate()} loading={analyze.isPending}>
          {latest ? "Run new analysis" : "Analyze evidence"}
        </Button>
      </div>
      {analyze.error && (
        <p role="alert" className="mt-4 text-sm text-[var(--color-critical)]">
          {analyze.error.message}
        </p>
      )}
      {result.isLoading && (
        <p className="mt-6 text-sm text-[var(--color-muted-text)]">Loading analysis…</p>
      )}
      {!result.isLoading && !latest && (
        <p className="mt-6 rounded-lg bg-[var(--color-info-bg)] p-4 text-sm">
          No analysis yet. Evidence is sanitized and checked for untrusted instructions before use.
        </p>
      )}
      {latest && <AnalysisResult value={latest} />}
    </section>
  );
}

function AnalysisResult({ value }: { value: AnalysisView }) {
  const output = asRecord(value.output);
  const quality = asRecord(value.quality);
  const claims = asRecords(output.likelyRootCauses);
  const diagnostics = asRecords(output.diagnosticActions);
  const mitigations = asRecords(output.mitigationActions);
  const missing = asStrings(output.missingInformation);
  return (
    <div className="mt-6 space-y-6">
      <div className="grid gap-4 sm:grid-cols-[1fr_auto]">
        <div>
          <p className="text-xs font-medium tracking-wide text-[var(--color-muted-text)] uppercase">
            Concise summary
          </p>
          <p className="mt-2 text-sm leading-6">{text(output.summary)}</p>
        </div>
        <div className="rounded-lg border border-[var(--color-border)] px-4 py-3 text-right">
          <p className="text-xs text-[var(--color-muted-text)]">Confidence</p>
          <p className="text-2xl font-semibold">{Math.round(number(output.confidence) * 100)}%</p>
        </div>
      </div>
      <AnalysisList title="Evidence-linked claims" values={claims} />
      <AnalysisList title="Recommended diagnostics" values={diagnostics} />
      <AnalysisList title="Recommended mitigations" values={mitigations} />
      <div>
        <h3 className="text-sm font-semibold">Missing context</h3>
        <ul className="mt-2 list-disc space-y-1 pl-5 text-sm text-[var(--color-secondary-text)]">
          {missing.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      </div>
      <dl className="grid gap-3 border-t border-[var(--color-border)] pt-4 text-xs text-[var(--color-muted-text)] sm:grid-cols-4">
        <Metric label="Provider" value={`${value.provider} · ${value.model}`} />
        <Metric label="Latency" value={`${value.latencyMs} ms`} />
        <Metric label="Tokens" value={`${value.inputTokens} in / ${value.outputTokens} out`} />
        <Metric label="Estimated cost" value={`$${value.estimatedCostUsd}`} />
        <Metric
          label="Evidence coverage"
          value={`${Math.round(number(quality.evidenceCoverage) * 100)}%`}
        />
        <Metric label="Prompt" value={`incident-analysis v${value.promptVersion}`} />
      </dl>
      {value.fallbackReason && (
        <p className="text-xs text-[var(--color-warning-text)]">
          Controlled fallback: {value.fallbackReason}
        </p>
      )}
    </div>
  );
}

function AnalysisList({ title, values }: { title: string; values: Record<string, unknown>[] }) {
  return (
    <div>
      <h3 className="text-sm font-semibold">{title}</h3>
      <ul className="mt-2 space-y-2">
        {values.map((item, index) => {
          const ids = asStrings(item.evidenceIds);
          return (
            <li
              key={`${text(item.statement ?? item.title)}-${index}`}
              className="rounded-lg bg-[var(--color-info-bg)] p-3 text-sm"
            >
              <div className="flex flex-wrap items-center gap-2">
                <span>{text(item.statement ?? item.title)}</span>
                {Boolean(item.hypothesis) && (
                  <StatusBadge variant="warning">Hypothesis</StatusBadge>
                )}
              </div>
              {item.rationale ? (
                <p className="mt-1 text-xs text-[var(--color-secondary-text)]">
                  {text(item.rationale)}
                </p>
              ) : null}
              {ids.length > 0 && (
                <p className="mt-2 font-mono text-[11px] text-[var(--color-muted-text)]">
                  Evidence: {ids.join(", ")}
                </p>
              )}
            </li>
          );
        })}
      </ul>
    </div>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt>{label}</dt>
      <dd className="mt-1 font-medium text-[var(--color-primary-text)]">{value}</dd>
    </div>
  );
}

function asRecord(value: unknown): Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : {};
}
function asRecords(value: unknown) {
  return Array.isArray(value) ? value.map(asRecord) : [];
}
function asStrings(value: unknown) {
  return Array.isArray(value)
    ? value.filter((item): item is string => typeof item === "string")
    : [];
}
function text(value: unknown) {
  return typeof value === "string" ? value : "Not provided";
}
function number(value: unknown) {
  return typeof value === "number" ? value : 0;
}
