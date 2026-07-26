"use client";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect } from "react";
import { api, authorizedStream } from "@/lib/api";
import { Button, Skeleton, StatusBadge } from "@/components/ui";

export default function ExecutionPage() {
  const { id } = useParams<{ id: string }>();
  const client = useQueryClient();
  const result = useQuery({
    queryKey: ["execution", id],
    queryFn: () => api.execution(id),
    refetchInterval: 15_000,
  });
  const retry = useMutation({
    mutationFn: () => api.retryExecution(id),
    onSuccess: () => client.invalidateQueries({ queryKey: ["execution", id] }),
  });
  useEffect(() => {
    const controller = new AbortController();
    void authorizedStream(`/api/executions/${id}/events`, controller.signal)
      .then(async (response) => {
        if (!response.ok || !response.body) return;
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        while (true) {
          const { done } = await reader.read();
          if (done) break;
          decoder.decode();
          void client.invalidateQueries({ queryKey: ["execution", id] });
        }
      })
      .catch(() => undefined);
    return () => controller.abort();
  }, [id, client]);
  if (result.isLoading)
    return (
      <div className="p-8">
        <Skeleton className="h-10 w-72" />
        <Skeleton className="mt-6 h-96 w-full" />
      </div>
    );
  if (result.error || !result.data)
    return (
      <div className="p-8">
        <p role="alert" className="text-[var(--color-critical)]">
          Execution timeline could not be loaded.
        </p>
        <Button className="mt-4" onClick={() => result.refetch()}>
          Retry
        </Button>
      </div>
    );
  const data = result.data;
  const terminal = [
    "SUCCEEDED",
    "PARTIALLY_SUCCEEDED",
    "FAILED",
    "TIMED_OUT",
    "CANCELLED",
  ].includes(data.execution.status);
  return (
    <div className="mx-auto max-w-5xl p-4 sm:p-6 lg:p-8">
      <header className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <Link
            href={`/dashboard/incidents/${data.execution.incidentId}`}
            className="text-sm text-[var(--color-secondary-text)] hover:underline"
          >
            ← Incident
          </Link>
          <div className="mt-3 flex items-center gap-3">
            <h1 className="text-3xl font-semibold">Execution timeline</h1>
            <StatusBadge variant={tone(data.execution.status)}>
              {data.execution.status.replaceAll("_", " ")}
            </StatusBadge>
          </div>
          <p className="mt-2 text-sm text-[var(--color-secondary-text)]">
            {data.execution.workflowKey} · version {data.execution.workflowVersion}
          </p>
        </div>
        {terminal && data.execution.status !== "SUCCEEDED" && (
          <Button onClick={() => retry.mutate()} loading={retry.isPending}>
            Retry execution
          </Button>
        )}
      </header>
      <section className="mt-8 rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] p-4 sm:p-6">
        <h2 className="text-lg font-semibold">Steps</h2>
        <ol className="mt-5 space-y-0">
          {data.steps.map((step, index) => (
            <li key={`${step.id}-${step.attempt}`} className="grid grid-cols-[28px_1fr] gap-3">
              <div className="flex flex-col items-center">
                <span className={`mt-1 h-3 w-3 rounded-full ${dot(step.status)}`} />
                {index < data.steps.length - 1 && (
                  <span className="min-h-16 w-px flex-1 bg-[var(--color-border)]" />
                )}
              </div>
              <article className="pb-6">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <h3 className="font-medium">{step.name}</h3>
                  <StatusBadge variant={tone(step.status)}>{step.status.toLowerCase()}</StatusBadge>
                </div>
                <p className="mt-1 text-xs text-[var(--color-muted-text)]">
                  {step.actionId} · attempt {step.attempt}
                </p>
                {step.errorMessage && (
                  <p className="mt-2 rounded-lg bg-[var(--color-critical-bg)] p-3 text-sm text-[var(--color-critical)]">
                    {step.errorMessage}
                  </p>
                )}
              </article>
            </li>
          ))}
        </ol>
      </section>
      <section className="mt-6 rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] p-4 sm:p-6">
        <h2 className="text-lg font-semibold">Event log</h2>
        <div className="mt-4 space-y-3">
          {data.events.map((event) => (
            <article
              key={event.id}
              className="grid gap-1 border-b border-[var(--color-border)] pb-3 last:border-0 sm:grid-cols-[180px_1fr]"
            >
              <time className="text-xs text-[var(--color-muted-text)]">
                {new Date(event.occurredAt).toLocaleString()}
              </time>
              <div>
                <p className="text-sm font-medium">{event.message}</p>
                <p className="text-xs text-[var(--color-secondary-text)]">
                  {event.type} · {event.status}
                </p>
              </div>
            </article>
          ))}
        </div>
      </section>
    </div>
  );
}
function tone(status: string): "success" | "critical" | "warning" | "info" {
  if (status === "SUCCEEDED" || status === "APPROVED") return "success";
  if (status === "FAILED" || status === "DENIED" || status === "TIMED_OUT") return "critical";
  if (status === "WAITING" || status === "PAUSED_FOR_APPROVAL") return "warning";
  return "info";
}
function dot(status: string) {
  const t = tone(status);
  return t === "success"
    ? "bg-[var(--color-success)]"
    : t === "critical"
      ? "bg-[var(--color-critical)]"
      : t === "warning"
        ? "bg-[var(--color-warning)]"
        : "bg-[var(--color-muted-text)]";
}
