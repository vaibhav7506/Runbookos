"use client";

import Link from "next/link";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { api } from "@/lib/api";
import { Button, StatusBadge } from "@/components/ui";

export default function RunbookLibraryPage() {
  const client = useQueryClient();
  const [creating, setCreating] = useState(false);
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const result = useQuery({ queryKey: ["runbooks"], queryFn: api.runbooks });
  const create = useMutation({
    mutationFn: () => api.createRunbook(name, description),
    onSuccess: () => {
      setName("");
      setDescription("");
      setCreating(false);
      client.invalidateQueries({ queryKey: ["runbooks"] });
    },
  });
  return (
    <div className="mx-auto max-w-6xl p-4 sm:p-6 lg:p-8">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="text-xs font-medium tracking-wide text-[var(--color-muted-text)] uppercase">
            Governed automation
          </p>
          <h1 className="mt-2 text-3xl font-semibold">Runbook library</h1>
          <p className="mt-2 text-sm text-[var(--color-secondary-text)]">
            Versioned response plans with explicit risk, role, timeout, retry, rollback, and
            environment controls.
          </p>
        </div>
        <div className="flex gap-2">
          <Link
            href="/dashboard/runbooks/policies"
            className="rounded-lg border border-[var(--color-border)] px-4 py-2 text-sm font-medium"
          >
            Edit policies
          </Link>
          <Button onClick={() => setCreating((value) => !value)}>New runbook</Button>
        </div>
      </header>
      {creating && (
        <form
          className="mt-6 grid gap-4 rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] p-5"
          onSubmit={(event) => {
            event.preventDefault();
            create.mutate();
          }}
        >
          <label className="grid gap-2 text-sm font-medium">
            Name
            <input
              value={name}
              onChange={(event) => setName(event.target.value)}
              className="rounded-lg border border-[var(--color-border)] bg-transparent px-3 py-2"
              maxLength={160}
              required
            />
          </label>
          <label className="grid gap-2 text-sm font-medium">
            Description
            <textarea
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              className="min-h-24 rounded-lg border border-[var(--color-border)] bg-transparent p-3"
            />
          </label>
          <div>
            <Button type="submit" loading={create.isPending}>
              Create draft
            </Button>
          </div>
        </form>
      )}
      <div className="mt-8 grid gap-4 md:grid-cols-2">
        {result.data?.map((runbook) => (
          <Link
            key={runbook.id}
            href={`/dashboard/runbooks/${runbook.id}`}
            className="rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] p-5 transition-colors hover:bg-[var(--color-hover)]"
          >
            <div className="flex items-center justify-between gap-3">
              <h2 className="font-semibold">{runbook.name}</h2>
              {runbook.latestStatus && (
                <StatusBadge variant={runbook.latestStatus === "PUBLISHED" ? "success" : "warning"}>
                  {runbook.latestStatus}
                </StatusBadge>
              )}
            </div>
            <p className="mt-2 text-sm text-[var(--color-secondary-text)]">
              {runbook.description || "No description"}
            </p>
            <p className="mt-5 text-xs text-[var(--color-muted-text)]">
              Version {runbook.latestVersion ?? "—"} · Updated{" "}
              {new Date(runbook.updatedAt).toLocaleString()}
            </p>
          </Link>
        ))}
      </div>
      {!result.isLoading && result.data?.length === 0 && (
        <p className="mt-12 text-center text-sm text-[var(--color-muted-text)]">
          Create the first governed runbook to begin.
        </p>
      )}
    </div>
  );
}
