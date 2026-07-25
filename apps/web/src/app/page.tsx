"use client";

import Link from "next/link";
import { useTheme } from "@/components/theme-provider";
import { Button } from "@/components/ui";

export default function LandingPage() {
  const { resolvedTheme, setTheme } = useTheme();

  return (
    <div className="min-h-screen bg-[var(--color-bg)]">
      {/* Navigation */}
      <nav className="mx-auto flex max-w-6xl items-center justify-between px-6 py-4">
        <Link href="/" className="flex items-center gap-2.5">
          <svg
            width="28"
            height="28"
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
          <span className="text-base font-semibold text-[var(--color-primary-text)]">
            RunbookOS
          </span>
        </Link>
        <div className="flex items-center gap-3">
          <button
            onClick={() => setTheme(resolvedTheme === "light" ? "dark" : "light")}
            className="cursor-pointer rounded-[var(--radius-md)] p-2 text-[var(--color-secondary-text)] transition-colors hover:bg-[var(--color-hover)]"
            aria-label="Toggle theme"
          >
            {resolvedTheme === "light" ? (
              <svg
                width="16"
                height="16"
                viewBox="0 0 16 16"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.3"
                strokeLinecap="round"
                aria-hidden="true"
              >
                <path d="M13.5 8.5a5.5 5.5 0 01-6-6 5.5 5.5 0 106 6z" />
              </svg>
            ) : (
              <svg
                width="16"
                height="16"
                viewBox="0 0 16 16"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.3"
                strokeLinecap="round"
                aria-hidden="true"
              >
                <path d="M8 1v1.5M8 13.5V15M1 8h1.5M13.5 8H15M3.05 3.05l1.06 1.06M11.89 11.89l1.06 1.06M3.05 12.95l1.06-1.06M11.89 4.11l1.06-1.06" />
                <circle cx="8" cy="8" r="3" />
              </svg>
            )}
          </button>
          <Link href="/login">
            <Button variant="secondary" size="sm">
              Sign in
            </Button>
          </Link>
        </div>
      </nav>

      {/* Hero */}
      <section className="mx-auto max-w-4xl px-6 pt-24 pb-16 text-center">
        <h1 className="mb-5 text-4xl leading-[1.1] font-bold tracking-tight text-[var(--color-primary-text)] sm:text-5xl">
          Human-Governed AI
          <br />
          Incident Response
        </h1>
        <p className="mx-auto mb-8 max-w-2xl text-lg leading-relaxed text-[var(--color-secondary-text)]">
          Receive production incident signals. Collect diagnostic evidence. Generate AI-grounded
          analysis. Recommend runbooks. Execute remediation with human approval.
        </p>
        <div className="flex items-center justify-center gap-3">
          <Link href="/login">
            <Button size="lg">Get Started</Button>
          </Link>
          <a href="https://github.com" target="_blank" rel="noopener noreferrer">
            <Button variant="secondary" size="lg">
              View on GitHub
            </Button>
          </a>
        </div>
      </section>

      {/* Architecture highlights */}
      <section className="mx-auto max-w-5xl px-6 py-16">
        <h2 className="mb-10 text-center text-xl font-semibold text-[var(--color-primary-text)]">
          Architecture
        </h2>
        <div className="grid grid-cols-1 gap-6 md:grid-cols-3">
          <FeatureCard
            title="Java Control Plane"
            description="Spring Boot handles authorization, policy enforcement, state management, and audit. AI is advisory — the policy engine decides."
            icon={
              <svg
                width="24"
                height="24"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.5"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
              >
                <rect x="3" y="3" width="18" height="18" rx="3" />
                <path d="M9 3v18M3 9h18" />
              </svg>
            }
          />
          <FeatureCard
            title="n8n Orchestration"
            description="Self-hosted n8n workflows collect evidence, run diagnostics, and execute approved actions — all gated by signed tokens."
            icon={
              <svg
                width="24"
                height="24"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.5"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
              >
                <circle cx="6" cy="6" r="3" />
                <circle cx="18" cy="12" r="3" />
                <circle cx="6" cy="18" r="3" />
                <path d="M9 6h6M9 18h6M6 9v6" />
              </svg>
            }
          />
          <FeatureCard
            title="Human-in-the-Loop"
            description="Read-only actions auto-execute. Reversible actions require approval. High-risk actions need elevated multi-user confirmation."
            icon={
              <svg
                width="24"
                height="24"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.5"
                strokeLinecap="round"
                strokeLinejoin="round"
                aria-hidden="true"
              >
                <circle cx="12" cy="12" r="9" />
                <path d="M9 12l2 2 4-4" />
              </svg>
            }
          />
        </div>
      </section>

      {/* Safety model */}
      <section className="mx-auto max-w-5xl border-t border-[var(--color-border)] px-6 py-16">
        <h2 className="mb-10 text-center text-xl font-semibold text-[var(--color-primary-text)]">
          Safety Model
        </h2>
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          <RiskCard
            level="READ_ONLY"
            description="Auto-execute after policy validation"
            color="success"
          />
          <RiskCard level="REVERSIBLE" description="Requires authorized approval" color="warning" />
          <RiskCard
            level="HIGH_RISK"
            description="Multi-user elevated approval required"
            color="critical"
          />
          <RiskCard level="PROHIBITED" description="Blocked — never executes" color="neutral" />
        </div>
      </section>

      {/* Technology stack */}
      <section className="mx-auto max-w-5xl border-t border-[var(--color-border)] px-6 py-16">
        <h2 className="mb-10 text-center text-xl font-semibold text-[var(--color-primary-text)]">
          Technology Stack
        </h2>
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-6">
          {[
            "Java 21",
            "Spring Boot",
            "Next.js",
            "TypeScript",
            "PostgreSQL",
            "Redis",
            "n8n",
            "Docker",
            "Tailwind CSS",
            "Radix UI",
            "Flyway",
            "OpenAPI",
          ].map((tech) => (
            <div
              key={tech}
              className="flex items-center justify-center rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-surface)] px-3 py-2.5 text-xs font-medium text-[var(--color-secondary-text)]"
            >
              {tech}
            </div>
          ))}
        </div>
      </section>

      {/* Footer */}
      <footer className="border-t border-[var(--color-border)] px-6 py-8">
        <div className="mx-auto flex max-w-5xl items-center justify-between">
          <p className="text-xs text-[var(--color-muted-text)]">RunbookOS — Open Source</p>
          <p className="text-xs text-[var(--color-muted-text)]">MIT License</p>
        </div>
      </footer>
    </div>
  );
}

function FeatureCard({
  title,
  description,
  icon,
}: {
  title: string;
  description: string;
  icon: React.ReactNode;
}) {
  return (
    <div className="rounded-[var(--radius-lg)] border border-[var(--color-border)] bg-[var(--color-surface)] p-5">
      <div className="mb-3 text-[var(--color-primary-text)]">{icon}</div>
      <h3 className="mb-1.5 text-sm font-semibold text-[var(--color-primary-text)]">{title}</h3>
      <p className="text-sm leading-relaxed text-[var(--color-secondary-text)]">{description}</p>
    </div>
  );
}

function RiskCard({
  level,
  description,
  color,
}: {
  level: string;
  description: string;
  color: "success" | "warning" | "critical" | "neutral";
}) {
  const colors = {
    success: "border-l-[var(--color-success)]",
    warning: "border-l-[var(--color-warning)]",
    critical: "border-l-[var(--color-critical)]",
    neutral: "border-l-[var(--color-muted-text)]",
  };

  return (
    <div
      className={`rounded-[var(--radius-md)] border border-l-2 border-[var(--color-border)] bg-[var(--color-surface)] p-4 ${colors[color]} `}
    >
      <p className="mb-1 font-mono text-xs font-semibold text-[var(--color-primary-text)]">
        {level}
      </p>
      <p className="text-xs text-[var(--color-secondary-text)]">{description}</p>
    </div>
  );
}
