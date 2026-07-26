"use client";
import { useEffect, useState } from "react";
export function SessionGuard({ children }: { children: React.ReactNode }) {
  const [expired, setExpired] = useState(false);
  useEffect(() => {
    const listener = () => setExpired(true);
    window.addEventListener("runbookos:session-expired", listener);
    return () => window.removeEventListener("runbookos:session-expired", listener);
  }, []);
  return (
    <>
      {children}
      {expired && (
        <div
          role="alertdialog"
          aria-modal="true"
          aria-labelledby="expired-title"
          className="fixed inset-0 z-50 grid place-items-center bg-black/30 p-4"
        >
          <div className="max-w-sm rounded-xl bg-[var(--color-surface)] p-6 shadow-[var(--shadow-dialog)]">
            <h2 id="expired-title" className="text-lg font-semibold">
              Session expired
            </h2>
            <p className="mt-2 text-sm text-[var(--color-secondary-text)]">
              Sign in again to continue. Your current page will remain available in browser history.
            </p>
            <a
              href="/login"
              className="mt-5 inline-flex rounded-lg bg-[var(--color-control)] px-4 py-2 text-sm text-[var(--color-control-text)]"
            >
              Sign in
            </a>
          </div>
        </div>
      )}
    </>
  );
}
