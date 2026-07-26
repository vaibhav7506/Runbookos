"use client";
import { useState } from "react";
import { api, ApiError } from "@/lib/api";
import { Button, Input } from "@/components/ui";

export default function OnboardingPage() {
  const [mode, setMode] = useState<"demo" | "integration">("demo");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPending(true);
    setError(null);
    try {
      const data = new FormData(event.currentTarget);
      const org = await api.createOrganization(String(data.get("name")), mode === "demo");
      await api.switchOrganization(org.id);
      window.location.href = "/dashboard/incidents";
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : "Could not create the workspace.");
      setPending(false);
    }
  }
  return (
    <main className="mx-auto flex min-h-screen max-w-2xl items-center p-4">
      <section className="w-full rounded-[var(--radius-xl)] border border-[var(--color-border)] bg-[var(--color-surface)] p-6 sm:p-10">
        <p className="text-xs font-medium tracking-widest text-[var(--color-muted-text)] uppercase">
          Workspace setup · 1 of 2
        </p>
        <h1 className="mt-3 text-3xl font-semibold">Create your response workspace</h1>
        <form onSubmit={submit} className="mt-8 space-y-6">
          <Input
            id="name"
            name="name"
            label="Organization name"
            placeholder="Acme Engineering"
            required
          />
          <fieldset>
            <legend className="mb-3 text-sm font-medium">How would you like to start?</legend>
            <div className="grid gap-3 sm:grid-cols-2">
              {(
                [
                  ["demo", "Demo Mode", "Seeded signals and simulated safe actions."],
                  [
                    "integration",
                    "Integration setup",
                    "Connect providers after the workspace is created.",
                  ],
                ] as const
              ).map(([value, title, copy]) => (
                <label
                  key={value}
                  className={`cursor-pointer rounded-xl border p-4 ${mode === value ? "border-[var(--color-primary-text)]" : "border-[var(--color-border)]"}`}
                >
                  <input
                    className="sr-only"
                    type="radio"
                    checked={mode === value}
                    onChange={() => setMode(value)}
                  />
                  <span className="block text-sm font-medium">{title}</span>
                  <span className="mt-1 block text-xs text-[var(--color-secondary-text)]">
                    {copy}
                  </span>
                </label>
              ))}
            </div>
          </fieldset>
          <div className="rounded-lg bg-[var(--color-info-bg)] p-4">
            <p className="text-sm font-medium">Invite teammates</p>
            <p className="mt-1 text-xs text-[var(--color-secondary-text)]">
              Available after setup. Invitations remain deliberately disabled until outbound email
              is configured.
            </p>
          </div>
          {error && (
            <p role="alert" className="text-sm text-[var(--color-critical)]">
              {error}
            </p>
          )}
          <Button type="submit" loading={pending}>
            Create workspace
          </Button>
        </form>
      </section>
    </main>
  );
}
