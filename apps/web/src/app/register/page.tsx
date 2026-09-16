"use client";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { api, ApiError } from "@/lib/api";
import { Button, Input } from "@/components/ui";
import { validateRegistration } from "@/lib/registration";

export default function RegisterPage() {
  const router = useRouter();
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    const data = new FormData(event.currentTarget);
    const displayName = String(data.get("displayName") ?? "").trim();
    const email = String(data.get("email") ?? "").trim();
    const password = String(data.get("password") ?? "");
    const nextErrors = validateRegistration({ displayName, email, password });
    setFieldErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;
    setPending(true);
    try {
      await api.signup(email, password, displayName);
      router.push("/onboarding");
    } catch (reason) {
      if (reason instanceof ApiError) {
        setFieldErrors(
          Object.fromEntries(reason.validationErrors.map((item) => [item.field, item.message]))
        );
        setError(
          reason.code === "EMAIL_ALREADY_REGISTERED"
            ? "An account already exists for this email. Sign in instead or use another email."
            : reason.validationErrors.length
              ? "Check the highlighted fields and try again."
              : reason.message
        );
      } else {
        setError("Could not create the account. Check your connection and try again.");
      }
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
        <form onSubmit={submit} className="mt-7 space-y-4" noValidate>
          <Input
            id="displayName"
            name="displayName"
            label="Name"
            autoComplete="name"
            error={fieldErrors.displayName}
            required
          />
          <Input
            id="email"
            name="email"
            label="Email address"
            type="email"
            autoComplete="email"
            hint="Personal and work email addresses are both accepted."
            error={fieldErrors.email}
            required
          />
          <Input
            id="password"
            name="password"
            label="Password"
            type="password"
            autoComplete="new-password"
            minLength={12}
            maxLength={128}
            hint="Use between 12 and 128 characters."
            error={fieldErrors.password}
            required
          />
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
