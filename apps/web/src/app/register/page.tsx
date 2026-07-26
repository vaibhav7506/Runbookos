"use client";
import Link from "next/link";
import { useState } from "react";
import { api, ApiError } from "@/lib/api";
import { Button, Input } from "@/components/ui";

export default function RegisterPage() {
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setPending(true);
    setError(null);
    const data = new FormData(event.currentTarget);
    try {
      await api.signup(
        String(data.get("email")),
        String(data.get("password")),
        String(data.get("displayName"))
      );
      window.location.href = "/onboarding";
    } catch (reason) {
      setError(reason instanceof ApiError ? reason.message : "Could not create the account.");
      setPending(false);
    }
  }
  return (
    <main className="flex min-h-screen items-center justify-center p-4">
      <section className="w-full max-w-md rounded-[var(--radius-xl)] border border-[var(--color-border)] bg-[var(--color-surface)] p-8">
        <p className="mb-2 text-xs font-medium tracking-widest text-[var(--color-muted-text)] uppercase">
          RunbookOS
        </p>
        <h1 className="text-2xl font-semibold">Create your account</h1>
        <p className="mt-2 text-sm text-[var(--color-secondary-text)]">
          Start with Demo Mode. No external credentials are required.
        </p>
        <form onSubmit={submit} className="mt-7 space-y-4">
          <Input id="displayName" name="displayName" label="Name" autoComplete="name" required />
          <Input
            id="email"
            name="email"
            label="Work email"
            type="email"
            autoComplete="email"
            required
          />
          <Input
            id="password"
            name="password"
            label="Password"
            type="password"
            autoComplete="new-password"
            minLength={12}
            required
          />
          <p className="text-xs text-[var(--color-muted-text)]">Use at least 12 characters.</p>
          {error && (
            <p role="alert" className="text-sm text-[var(--color-critical)]">
              {error}
            </p>
          )}
          <Button className="w-full" type="submit" loading={pending}>
            Continue
          </Button>
        </form>
        <p className="mt-5 text-center text-sm text-[var(--color-secondary-text)]">
          Already registered?{" "}
          <Link href="/login" className="font-medium text-[var(--color-primary-text)]">
            Sign in
          </Link>
        </p>
      </section>
    </main>
  );
}
