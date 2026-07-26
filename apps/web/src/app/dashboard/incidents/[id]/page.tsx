"use client";
import { useParams, useRouter } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { api, ApiError } from "@/lib/api";
import { Button, Skeleton, StatusBadge } from "@/components/ui";
import { AnalysisPanel } from "@/components/analysis-panel";
export default function IncidentDetailPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const client = useQueryClient();
  const [comment, setComment] = useState("");
  const result = useQuery({ queryKey: ["incident", id], queryFn: () => api.incident(id) });
  const transition = useMutation({
    mutationFn: (status: string) => api.transition(id, status),
    onSuccess: () => client.invalidateQueries({ queryKey: ["incident", id] }),
  });
  const addComment = useMutation({
    mutationFn: () => api.comment(id, comment),
    onSuccess: () => {
      setComment("");
      client.invalidateQueries({ queryKey: ["incident", id] });
    },
  });
  const execute = useMutation({
    mutationFn: () => api.createExecution(id),
    onSuccess: (value) => router.push(`/dashboard/executions/${value.id}`),
  });
  if (result.isLoading)
    return (
      <div className="p-8">
        <Skeleton className="h-10 w-2/3" />
        <Skeleton className="mt-6 h-80 w-full" />
      </div>
    );
  if (result.error)
    return (
      <div className="p-8">
        <p role="alert" className="text-[var(--color-critical)]">
          {result.error instanceof ApiError
            ? result.error.message
            : "Incident could not be loaded."}
        </p>
        <Button className="mt-4" onClick={() => result.refetch()}>
          Retry
        </Button>
      </div>
    );
  const data = result.data;
  if (!data) return null;
  const i = data.incident;
  return (
    <div className="mx-auto max-w-7xl p-4 sm:p-6 lg:p-8">
      <header className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <div className="flex flex-wrap items-center gap-2">
            <StatusBadge
              variant={
                i.severity === "SEV1" ? "critical" : i.severity === "SEV2" ? "warning" : "info"
              }
            >
              {i.severity}
            </StatusBadge>
            <span className="text-xs text-[var(--color-muted-text)]">{i.affectedService}</span>
          </div>
          <h1 className="mt-3 text-3xl font-semibold">{i.title}</h1>
          <p className="mt-2 max-w-3xl text-sm text-[var(--color-secondary-text)]">
            {i.summary ?? "No summary has been added."}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Button
            variant="secondary"
            onClick={() => transition.mutate("INVESTIGATING")}
            loading={transition.isPending}
          >
            Start investigation
          </Button>
          <Button onClick={() => execute.mutate()} loading={execute.isPending}>
            Run triage workflow
          </Button>
        </div>
      </header>
      <div className="mt-8 grid gap-6 lg:grid-cols-[minmax(0,1fr)_340px]">
        <main className="space-y-6">
          <Panel title="Timeline">
            <ol className="space-y-5">
              <Timeline
                title="Detected"
                time={i.firstDetectedAt}
                copy={`${i.signalCount} grouped signal${i.signalCount === 1 ? "" : "s"}`}
              />
              <Timeline
                title={i.status.replaceAll("_", " ")}
                time={i.lastSignalAt}
                copy="Current incident state"
              />
            </ol>
          </Panel>
          <Panel title="Evidence">
            {data.evidence.length === 0 ? (
              <Empty copy="Evidence will appear as diagnostic workflows complete." />
            ) : (
              <div className="space-y-3">
                {data.evidence.map((e) => (
                  <article
                    key={e.id}
                    className="rounded-lg border border-[var(--color-border)] p-4"
                  >
                    <p className="text-xs text-[var(--color-muted-text)]">{e.kind}</p>
                    <h3 className="mt-1 font-medium">{e.title}</h3>
                    <pre className="mt-3 overflow-auto text-xs text-[var(--color-secondary-text)]">
                      {JSON.stringify(e.content, null, 2)}
                    </pre>
                  </article>
                ))}
              </div>
            )}
          </Panel>
          <AnalysisPanel incidentId={id} />
        </main>
        <aside className="space-y-6">
          <Panel title="Incident details">
            <dl className="space-y-3 text-sm">
              <Detail label="Status" value={i.status.replaceAll("_", " ")} />
              <Detail label="Priority" value={`P${i.priority}`} />
              <Detail label="Assignee" value={i.assigneeUserId ?? "Unassigned"} />
              <Detail label="Last signal" value={new Date(i.lastSignalAt).toLocaleString()} />
            </dl>
          </Panel>
          <Panel title="Comments">
            <div className="space-y-3">
              {data.comments.map((c) => (
                <div key={c.id} className="rounded-lg bg-[var(--color-info-bg)] p-3">
                  <p className="text-sm">{c.body}</p>
                  <p className="mt-2 text-xs text-[var(--color-muted-text)]">
                    {new Date(c.createdAt).toLocaleString()}
                  </p>
                </div>
              ))}
            </div>
            <form
              onSubmit={(e) => {
                e.preventDefault();
                if (comment.trim()) addComment.mutate();
              }}
              className="mt-4"
            >
              <label htmlFor="comment" className="text-sm font-medium">
                Add comment
              </label>
              <textarea
                id="comment"
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                className="mt-2 min-h-24 w-full rounded-lg border border-[var(--color-border)] bg-transparent p-3 text-sm"
                required
              />
              <Button className="mt-2" type="submit" loading={addComment.isPending}>
                Post comment
              </Button>
            </form>
          </Panel>
        </aside>
      </div>
    </div>
  );
}
function Panel({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section className="rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] p-5">
      <h2 className="mb-4 text-lg font-semibold">{title}</h2>
      {children}
    </section>
  );
}
function Timeline({ title, time, copy }: { title: string; time: string; copy: string }) {
  return (
    <li className="grid grid-cols-[12px_1fr] gap-3">
      <span className="mt-1.5 h-2.5 w-2.5 rounded-full bg-[var(--color-control)]" />
      <div>
        <p className="text-sm font-medium">{title}</p>
        <p className="text-xs text-[var(--color-secondary-text)]">{copy}</p>
        <time className="text-xs text-[var(--color-muted-text)]">
          {new Date(time).toLocaleString()}
        </time>
      </div>
    </li>
  );
}
function Detail({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between gap-4">
      <dt className="text-[var(--color-muted-text)]">{label}</dt>
      <dd className="text-right">{value}</dd>
    </div>
  );
}
function Empty({ copy }: { copy: string }) {
  return <p className="py-8 text-center text-sm text-[var(--color-muted-text)]">{copy}</p>;
}
