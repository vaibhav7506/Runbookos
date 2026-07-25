"use client";

import Link from "next/link";
import { useState } from "react";
import { Button, Input } from "@/components/ui";

export default function LoginPage() {
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setIsLoading(true);
    // Authentication will be implemented in Phase 2
    setTimeout(() => {
      window.location.href = "/dashboard";
    }, 800);
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-[var(--color-bg)] p-4">
      <div className="w-full max-w-sm">
        {/* Logo */}
        <div className="mb-8 flex items-center justify-center gap-2.5">
          <svg
            width="32"
            height="32"
            viewBox="0 0 32 32"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
            aria-hidden="true"
          >
            <rect x="4" y="4" width="24" height="24" rx="5" stroke="currentColor" strokeWidth="2" />
            <path
              d="M10 10h12M10 15h8M10 20h9"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
            />
            <circle cx="22" cy="19" r="3" stroke="currentColor" strokeWidth="1.5" />
            <path
              d="M24.2 21.2l2 2"
              stroke="currentColor"
              strokeWidth="1.5"
              strokeLinecap="round"
            />
          </svg>
          <span className="text-lg font-semibold text-[var(--color-primary-text)]">RunbookOS</span>
        </div>

        {/* Card */}
        <div className="rounded-[var(--radius-xl)] border border-[var(--color-border)] bg-[var(--color-surface)] p-8 shadow-[var(--shadow-md)]">
          <h1 className="mb-1.5 text-xl font-semibold text-[var(--color-primary-text)]">Sign in</h1>
          <p className="mb-6 text-sm text-[var(--color-secondary-text)]">
            Access your incident response workspace.
          </p>

          <form onSubmit={handleSubmit} className="flex flex-col gap-4">
            <Input
              label="Email"
              id="email"
              type="email"
              autoComplete="email"
              placeholder="you@example.com"
              required
            />
            <div>
              <Input
                label="Password"
                id="password"
                type="password"
                autoComplete="current-password"
                placeholder="••••••••"
                required
              />
              <div className="mt-1.5 text-right">
                <Link
                  href="#"
                  className="text-xs text-[var(--color-secondary-text)] transition-colors hover:text-[var(--color-primary-text)]"
                >
                  Forgot password?
                </Link>
              </div>
            </div>

            <Button type="submit" className="mt-2 w-full" loading={isLoading}>
              Sign in
            </Button>
          </form>
        </div>

        {/* Create account link */}
        <p className="mt-4 text-center text-sm text-[var(--color-secondary-text)]">
          Don&apos;t have an account?{" "}
          <Link
            href="/register"
            className="font-medium text-[var(--color-primary-text)] hover:underline"
          >
            Create one
          </Link>
        </p>
      </div>
    </div>
  );
}
