"use client";

import type { ProviderRequest } from "@runbookos/api-client";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { api } from "@/lib/api";
import { Button, StatusBadge } from "@/components/ui";

export default function SettingsPage() {
  const client = useQueryClient();
  const providers = useQuery({ queryKey: ["ai-providers"], queryFn: api.aiProviders });
  const [provider, setProvider] = useState<ProviderRequest["provider"]>("OPENAI");
  const [model, setModel] = useState("");
  const [baseUrl, setBaseUrl] = useState("");
  const [apiKey, setApiKey] = useState("");
  const configure = useMutation({
    mutationFn: () =>
      api.configureAiProvider({
        provider,
        model,
        baseUrl,
        apiKey,
        priority: (providers.data?.length ?? 0) + 1,
        timeoutSeconds: 30,
        maxRetries: 1,
        inputTokenBudget: 12000,
        outputTokenBudget: 2000,
        costBudgetUsd: 1,
      }),
    onSuccess: () => {
      setApiKey("");
      client.invalidateQueries({ queryKey: ["ai-providers"] });
    },
  });
  return (
    <div className="mx-auto max-w-5xl p-4 sm:p-6 lg:p-8">
      <h1 className="text-3xl font-semibold">Settings</h1>
      <section className="mt-8 rounded-xl border border-[var(--color-border)] bg-[var(--color-surface)] p-5">
        <h2 className="text-lg font-semibold">AI providers</h2>
        <p className="mt-1 text-sm text-[var(--color-secondary-text)]">
          Bring your own key. Keys are encrypted and can never be retrieved after saving. Demo Mode
          requires no provider.
        </p>
        <form
          onSubmit={(event) => {
            event.preventDefault();
            configure.mutate();
          }}
          className="mt-6 grid gap-4 sm:grid-cols-2"
        >
          <label className="grid gap-2 text-sm font-medium">
            Provider
            <select
              value={provider}
              onChange={(event) => setProvider(event.target.value as ProviderRequest["provider"])}
              className="rounded-lg border border-[var(--color-border)] bg-transparent px-3 py-2"
            >
              <option>OPENAI</option>
              <option>ANTHROPIC</option>
              <option>GEMINI</option>
              <option>GROQ</option>
              <option>OPENAI_COMPATIBLE</option>
            </select>
          </label>
          <label className="grid gap-2 text-sm font-medium">
            Model
            <input
              value={model}
              onChange={(event) => setModel(event.target.value)}
              required
              className="rounded-lg border border-[var(--color-border)] bg-transparent px-3 py-2"
            />
          </label>
          <label className="grid gap-2 text-sm font-medium">
            Compatible base URL (optional)
            <input
              type="url"
              value={baseUrl}
              onChange={(event) => setBaseUrl(event.target.value)}
              className="rounded-lg border border-[var(--color-border)] bg-transparent px-3 py-2"
            />
          </label>
          <label className="grid gap-2 text-sm font-medium">
            API key
            <input
              type="password"
              value={apiKey}
              onChange={(event) => setApiKey(event.target.value)}
              required
              autoComplete="new-password"
              className="rounded-lg border border-[var(--color-border)] bg-transparent px-3 py-2"
            />
          </label>
          <div>
            <Button type="submit" loading={configure.isPending}>
              Save encrypted provider
            </Button>
          </div>
        </form>
        <div className="mt-8 space-y-3">
          {providers.data?.map((item) => (
            <div
              key={item.id}
              className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-[var(--color-border)] p-4"
            >
              <div>
                <p className="font-medium">
                  {item.provider} · {item.model}
                </p>
                <p className="mt-1 text-xs text-[var(--color-muted-text)]">
                  Key fingerprint {item.keyFingerprint} · priority {item.priority} · $
                  {item.costBudgetUsd} ceiling
                </p>
              </div>
              <StatusBadge variant={item.enabled ? "success" : "neutral"}>
                {item.enabled ? "Enabled" : "Disabled"}
              </StatusBadge>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
