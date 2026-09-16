"use client";

import { useParams, useRouter } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { api, ApiError } from "@/lib/api";
import { Button, EmptyState, Skeleton, StatusBadge } from "@/components/ui";
import { AnalysisPanel } from "@/components/analysis-panel";

const progression = [
  "DETECTED",
  "TRIAGED",
  "INVESTIGATING",
  "AWAITING_APPROVAL",
  "MITIGATING",
  "MONITORING",
  "RESOLVED",
] as const;

export default function IncidentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const client = useQueryClient();
  const [comment, setComment] = useState("");
  const [notice, setNotice] = useState("");
  const result = useQuery({ queryKey: ["incident", id], queryFn: () => api.incident(id) });
  const analyses = useQuery({ queryKey: ["analyses", id], queryFn: () => api.analyses(id) });
  const postmortem = useQuery({
    queryKey: ["postmortem", id],
    queryFn: () => api.postmortem(id),
    enabled:
      result.data?.incident.status === "RESOLVED" || result.data?.incident.status === "CLOSED",
    retry: false,
  });
  const transition = useMutation({
    mutationFn: (status: string) => api.transition(id, status),
    onSuccess: async () => {
      setNotice("Incident state updated.");
      await client.invalidateQueries({ queryKey: ["incident", id] });
    },
  });
  const addComment = useMutation({
    mutationFn: () => api.comment(id, comment),
    onSuccess: async () => {
      setComment("");
      setNotice("Comment added.");
      await client.invalidateQueries({ queryKey: ["incident", id] });
    },
  });
  const execute = useMutation({
    mutationFn: () => api.createExecution(id),
    onSuccess: (value) => router.push(`/dashboard/executions/${value.id}`),
  });
  const generatePostmortem = useMutation({
    mutationFn: () => api.generatePostmortem(id),
    onSuccess: async () => {
      setNotice("Postmortem generated.");
      await client.invalidateQueries({ queryKey: ["postmortem", id] });
    },
  });

  if (result.isLoading) return <LoadingIncident />;
  if (result.error) return <LoadError error={result.error} retry={() => result.refetch()} />;
  const data = result.data;
  if (!data) return null;
  const incident = data.incident;
  const currentIndex = progression.indexOf(incident.status as (typeof progression)[number]);
  const next =
    currentIndex >= 0 && currentIndex < progression.length - 1
      ? progression[currentIndex + 1]
      : null;
  const timeline = buildTimeline(incident, data.evidence.length, analyses.data?.length ?? 0);
  const mutationError = (transition.error ??
    addComment.error ??
    execute.error ??
    generatePostmortem.error) as Error | null;

  return (
    <main className="mx-auto max-w-[1600px] p-4 sm:p-6 lg:p-8">
      <header className="flex flex-col gap-5 border-b border-[var(--color-border)] pb-7 xl:flex-row xl:items-start xl:justify-between">
        <div className="min-w-0">
          <div className="flex flex-wrap items-center gap-2">
            <StatusBadge
              variant={
                incident.severity === "SEV1"
                  ? "critical"
                  : incident.severity === "SEV2"
                    ? "warning"
                    : "info"
              }
            >
              {incident.severity}
            </StatusBadge>
            <StatusBadge variant={incident.status === "RESOLVED" ? "success" : "info"}>
              {pretty(incident.status)}
            </StatusBadge>
            <span className="text-xs text-[var(--color-muted-text)]">
              {incident.affectedService}
            </span>
          </div>
          <h1 className="mt-3 max-w-4xl text-3xl font-semibold tracking-tight">{incident.title}</h1>
          <p className="mt-2 max-w-3xl text-sm leading-6 text-[var(--color-secondary-text)]">
            {incident.summary || "No summary has been added."}
          </p>
        </div>
        <div className="flex flex-wrap gap-2 print:hidden">
          <Button
            variant="secondary"
            onClick={async () => {
              await navigator.clipboard.writeText(window.location.href);
              setNotice("Incident link copied.");
            }}
          >
            Copy link
          </Button>
          <Button onClick={() => execute.mutate()} loading={execute.isPending}>
            Run triage workflow
          </Button>
        </div>
      </header>

      <div aria-live="polite" className="min-h-6 pt-3 text-sm text-[var(--color-success)]">
        {notice}
      </div>
      {mutationError && (
        <p role="alert" className="mb-4 text-sm text-[var(--color-critical)]">
          {mutationError instanceof ApiError
            ? mutationError.message
            : "The action could not be completed."}
        </p>
      )}

      <div className="mt-3 grid gap-6 xl:grid-cols-[minmax(0,1.25fr)_minmax(300px,0.8fr)_280px]">
        <section className="min-w-0 space-y-6" aria-label="Incident workspace">
          <Panel
            title="Incident timeline"
            subtitle="Detection through learning, with exact times and source context."
          >
            <ol className="relative space-y-0 before:absolute before:top-3 before:bottom-3 before:left-[7px] before:w-px before:bg-[var(--color-border)]">
              {timeline.map(({ key, ...event }) => (
                <TimelineEvent key={key} {...event} />
              ))}
            </ol>
          </Panel>
          <Panel
            title="Responder conversation"
            subtitle="Comments are plain text and rendered without executable markup."
          >
            {data.comments.length ? (
              <div className="space-y-3">
                {data.comments.map((entry) => (
                  <article key={entry.id} className="rounded-xl bg-[var(--color-info-bg)] p-4">
                    <p className="text-sm leading-6">{entry.body}</p>
                    <ExactTime value={entry.createdAt} />
                  </article>
                ))}
              </div>
            ) : (
              <p className="py-4 text-sm text-[var(--color-muted-text)]">No responder notes yet.</p>
            )}
            <form
              onSubmit={(event) => {
                event.preventDefault();
                if (comment.trim()) addComment.mutate();
              }}
              className="mt-5"
            >
              <label htmlFor="comment" className="text-sm font-medium">
                Add responder note
              </label>
              <textarea
                id="comment"
                value={comment}
                onChange={(event) => setComment(event.target.value)}
                minLength={1}
                maxLength={4000}
                className="mt-2 min-h-24 w-full rounded-xl border border-[var(--color-border)] bg-transparent p-3 text-sm outline-none focus:border-[var(--color-control)]"
                required
              />
              <div className="mt-2 flex items-center justify-between">
                <span className="text-xs text-[var(--color-muted-text)]">
                  {comment.length}/4000
                </span>
                <Button type="submit" loading={addComment.isPending}>
                  Post note
                </Button>
              </div>
            </form>
          </Panel>
          {postmortem.data && <Postmortem value={postmortem.data} />}
        </section>

        <aside className="min-w-0 space-y-6" aria-label="Evidence and analysis drawer">
          <Panel title="Evidence drawer" subtitle={`${data.evidence.length} grounded items`}>
            {data.evidence.length ? (
              <div className="space-y-3">
                {data.evidence.map((evidence) => (
                  <details
                    key={evidence.id}
                    className="group rounded-xl border border-[var(--color-border)] p-4"
                  >
                    <summary className="cursor-pointer list-none">
                      <p className="text-[11px] font-semibold tracking-wider text-[var(--color-muted-text)] uppercase">
                        {evidence.kind}
                      </p>
                      <h3 className="mt-1 text-sm font-medium">{evidence.title}</h3>
                      <ExactTime value={evidence.collectedAt} />
                    </summary>
                    <pre className="mt-4 max-h-72 overflow-auto rounded-lg bg-[var(--color-bg)] p-3 text-[11px] break-words whitespace-pre-wrap text-[var(--color-secondary-text)]">
                      {JSON.stringify(evidence.content, null, 2)}
                    </pre>
                  </details>
                ))}
              </div>
            ) : (
              <EmptyState
                title="No evidence yet"
                description="Run the diagnostic workflow to collect bounded evidence."
              />
            )}
          </Panel>
          <AnalysisPanel incidentId={id} />
        </aside>

        <aside
          className="space-y-6 xl:sticky xl:top-4 xl:self-start"
          aria-label="Contextual actions"
        >
          <Panel title="Context">
            <dl className="space-y-3 text-sm">
              <Detail label="Priority" value={`P${incident.priority}`} />
              <Detail label="Signals" value={String(incident.signalCount)} />
              <Detail label="Assignee" value={incident.assigneeUserId || "Unassigned"} />
              <Detail label="Detected" value={relativeTime(incident.firstDetectedAt)} />
            </dl>
          </Panel>
          <Panel title="Next action">
            {next ? (
              <>
                <p className="text-sm text-[var(--color-secondary-text)]">
                  Advance to{" "}
                  <strong className="text-[var(--color-primary-text)]">{pretty(next)}</strong> when
                  the incident record supports it.
                </p>
                <Button
                  className="mt-4 w-full"
                  loading={transition.isPending}
                  onClick={() => transition.mutate(next)}
                >
                  {pretty(next)}
                </Button>
              </>
            ) : (
              <p className="text-sm text-[var(--color-secondary-text)]">
                The response lifecycle is complete.
              </p>
            )}
            {(incident.status === "RESOLVED" || incident.status === "CLOSED") &&
              !postmortem.data && (
                <Button
                  className="mt-3 w-full"
                  variant="secondary"
                  loading={generatePostmortem.isPending}
                  onClick={() => generatePostmortem.mutate()}
                >
                  Generate postmortem
                </Button>
              )}
          </Panel>
          <Panel title="Safety boundary">
            <p className="text-xs leading-5 text-[var(--color-secondary-text)]">
              AI recommendations are advisory. Reversible actions pause for an authorized approval;
              prohibited actions cannot execute.
            </p>
          </Panel>
        </aside>
      </div>
    </main>
  );
}

function buildTimeline(
  incident: { status: string; firstDetectedAt: string; lastSignalAt: string },
  evidenceCount: number,
  analysisCount: number
) {
  const reached = (status: string) =>
    progression.indexOf(incident.status as (typeof progression)[number]) >=
    progression.indexOf(status as (typeof progression)[number]);
  return [
    event(
      "detected",
      "Detection",
      incident.firstDetectedAt,
      "Signal ingestion",
      "Integration",
      "COMPLETED",
      "Inbound signal accepted and grouped."
    ),
    event(
      "evidence",
      "Evidence collected",
      incident.lastSignalAt,
      "Diagnostic workflow",
      "n8n",
      evidenceCount ? "COMPLETED" : "PENDING",
      `${evidenceCount} evidence items available.`
    ),
    event(
      "analysis",
      "AI analysis",
      incident.lastSignalAt,
      "Grounded analysis",
      "AI provider",
      analysisCount ? "COMPLETED" : "PENDING",
      `${analysisCount} validated analyses available.`
    ),
    event(
      "policy",
      "Policy decision",
      incident.lastSignalAt,
      "Java policy engine",
      "Control plane",
      reached("AWAITING_APPROVAL") ? "COMPLETED" : "PENDING",
      "Authorization remains deterministic and outside the model."
    ),
    event(
      "approval-request",
      "Approval requested",
      incident.lastSignalAt,
      "Runbook execution",
      "Control plane",
      reached("AWAITING_APPROVAL") ? "COMPLETED" : "PENDING",
      "Reversible action is gated."
    ),
    event(
      "approval-decision",
      "Approval decision",
      incident.lastSignalAt,
      "Authorized responder",
      "Application",
      reached("MITIGATING") ? "COMPLETED" : "PENDING",
      "Decision is identity-bound and audited."
    ),
    event(
      "action",
      "Action executed",
      incident.lastSignalAt,
      "Simulated rollback",
      "n8n",
      reached("MONITORING") ? "COMPLETED" : "PENDING",
      "Only the approved action identifier may execute."
    ),
    event(
      "monitoring",
      "Recovery monitoring",
      incident.lastSignalAt,
      "Health checks",
      "Integration",
      reached("MONITORING") ? "ACTIVE" : "PENDING",
      "Recovery signals are compared with the baseline."
    ),
    event(
      "resolution",
      "Resolution & postmortem",
      incident.lastSignalAt,
      "Incident owner",
      "Application",
      reached("RESOLVED") ? "COMPLETED" : "PENDING",
      "Learning artifact is generated only after resolution."
    ),
  ];
}
function event(
  key: string,
  title: string,
  time: string,
  actor: string,
  source: string,
  status: string,
  detail: string
) {
  return { key, title, time, actor, source, status, detail };
}
function TimelineEvent({ title, time, actor, source, status, detail }: ReturnType<typeof event>) {
  return (
    <li className="relative grid grid-cols-[16px_1fr] gap-4 pb-6 last:pb-0">
      <span
        className={`relative z-10 mt-1 h-4 w-4 rounded-full border-4 border-[var(--color-surface)] ${status === "COMPLETED" ? "bg-[var(--color-success)]" : status === "ACTIVE" ? "bg-[var(--color-warning)]" : "bg-[var(--color-border-elevated)]"}`}
      />
      <details className="group">
        <summary className="cursor-pointer list-none">
          <div className="flex flex-wrap items-start justify-between gap-2">
            <div>
              <h3 className="text-sm font-medium">{title}</h3>
              <p className="mt-0.5 text-xs text-[var(--color-muted-text)]">
                {actor} · {source}
              </p>
            </div>
            <StatusBadge
              variant={
                status === "COMPLETED" ? "success" : status === "ACTIVE" ? "warning" : "neutral"
              }
            >
              {status}
            </StatusBadge>
          </div>
          <ExactTime value={time} />
        </summary>
        <p className="mt-2 rounded-lg bg-[var(--color-info-bg)] p-3 text-xs leading-5 text-[var(--color-secondary-text)]">
          {detail}
        </p>
      </details>
    </li>
  );
}
function Panel({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle?: string;
  children: React.ReactNode;
}) {
  return (
    <section className="rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-5">
      <div className="mb-5">
        <h2 className="text-base font-semibold">{title}</h2>
        {subtitle && <p className="mt-1 text-xs text-[var(--color-muted-text)]">{subtitle}</p>}
      </div>
      {children}
    </section>
  );
}
function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-4">
      <dt className="text-[var(--color-muted-text)]">{label}</dt>
      <dd className="max-w-[55%] truncate text-right">{value}</dd>
    </div>
  );
}
function ExactTime({ value }: { value: string }) {
  return (
    <time
      dateTime={value}
      title={new Date(value).toISOString()}
      className="mt-1 block text-[11px] text-[var(--color-muted-text)]"
    >
      {relativeTime(value)} ·{" "}
      {new Intl.DateTimeFormat(undefined, { dateStyle: "medium", timeStyle: "medium" }).format(
        new Date(value)
      )}
    </time>
  );
}
function relativeTime(value: string) {
  const seconds = Math.round((new Date(value).getTime() - Date.now()) / 1000);
  const formatter = new Intl.RelativeTimeFormat(undefined, { numeric: "auto" });
  if (Math.abs(seconds) < 60) return formatter.format(seconds, "second");
  const minutes = Math.round(seconds / 60);
  if (Math.abs(minutes) < 60) return formatter.format(minutes, "minute");
  const hours = Math.round(minutes / 60);
  if (Math.abs(hours) < 24) return formatter.format(hours, "hour");
  return formatter.format(Math.round(hours / 24), "day");
}
function pretty(value: string) {
  return value
    .replaceAll("_", " ")
    .toLowerCase()
    .replace(/^\w/, (letter) => letter.toUpperCase());
}
function LoadingIncident() {
  return (
    <div className="mx-auto max-w-7xl p-8">
      <Skeleton className="h-10 w-2/3" />
      <div className="mt-8 grid gap-6 lg:grid-cols-3">
        <Skeleton className="h-96 lg:col-span-2" />
        <Skeleton className="h-96" />
      </div>
    </div>
  );
}
function LoadError({ error, retry }: { error: Error; retry: () => void }) {
  return (
    <div className="p-8">
      <EmptyState
        title="Incident unavailable"
        description={
          error instanceof ApiError
            ? `${error.message} · ${error.correlationId || "no reference"}`
            : "The incident could not be loaded."
        }
        action={<Button onClick={retry}>Retry</Button>}
      />
    </div>
  );
}
function Postmortem({ value }: { value: NonNullable<Awaited<ReturnType<typeof api.postmortem>>> }) {
  return (
    <article className="print-postmortem rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface)] p-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-xs font-semibold tracking-wider text-[var(--color-muted-text)] uppercase">
            Learning artifact
          </p>
          <h2 className="mt-2 text-xl font-semibold">{value.title}</h2>
        </div>
        <Button
          variant="secondary"
          size="sm"
          className="print:hidden"
          onClick={() => window.print()}
        >
          Print
        </Button>
      </div>
      <div className="mt-6 grid gap-6 sm:grid-cols-2">
        <PostmortemSection title="Summary" copy={value.summary} />
        <PostmortemSection title="Impact" copy={value.impact} />
        <PostmortemSection title="Root cause" copy={value.rootCause} />
        <PostmortemSection title="Resolution" copy={value.resolution} />
      </div>
      <div className="mt-6">
        <h3 className="text-sm font-semibold">Follow-up actions</h3>
        <ul className="mt-2 list-disc space-y-1 pl-5 text-sm text-[var(--color-secondary-text)]">
          {value.followUpActions.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      </div>
    </article>
  );
}
function PostmortemSection({ title, copy }: { title: string; copy: string }) {
  return (
    <section>
      <h3 className="text-sm font-semibold">{title}</h3>
      <p className="mt-2 text-sm leading-6 text-[var(--color-secondary-text)]">{copy}</p>
    </section>
  );
}
