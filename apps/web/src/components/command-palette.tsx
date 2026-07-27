"use client";

import * as Dialog from "@radix-ui/react-dialog";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useMemo, useState } from "react";

const destinations = [
  ["Overview", "/dashboard", "Operational health and activity"],
  ["Incidents", "/dashboard/incidents", "Search, triage, and resolve incidents"],
  ["Approvals", "/dashboard/approvals", "Review governed actions"],
  ["Runbooks", "/dashboard/runbooks", "Versioned response procedures"],
  ["Integrations", "/dashboard/integrations", "Connection setup and health"],
  ["Operational health", "/dashboard/operations", "Dead letters and platform health"],
  ["Audit log", "/dashboard/audit", "Integrity-protected security activity"],
  ["Settings", "/dashboard/settings", "AI providers and preferences"],
] as const;

export function CommandPalette() {
  const pathname = usePathname();
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const [initialRecent] = useState<string[]>(() =>
    typeof window === "undefined"
      ? []
      : (JSON.parse(localStorage.getItem("runbookos_recent_routes") ?? "[]") as string[])
  );
  const recent = useMemo(
    () => [pathname, ...initialRecent.filter((item) => item !== pathname)].slice(0, 5),
    [pathname, initialRecent]
  );
  useEffect(() => {
    const key = "runbookos_recent_routes";
    localStorage.setItem(key, JSON.stringify(recent));
  }, [recent]);
  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === "k") {
        event.preventDefault();
        setOpen((value) => !value);
      }
      if (event.key === "/" && event.target === document.body) {
        event.preventDefault();
        setOpen(true);
      }
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);
  const results = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    if (!normalized) {
      return destinations
        .filter(([, href]) => recent.includes(href))
        .sort((a, b) => recent.indexOf(a[1]) - recent.indexOf(b[1]));
    }
    return destinations.filter(([label, , copy]) =>
      `${label} ${copy}`.toLowerCase().includes(normalized)
    );
  }, [query, recent]);
  return (
    <Dialog.Root open={open} onOpenChange={setOpen}>
      <Dialog.Trigger asChild>
        <button
          className="hidden min-w-[220px] cursor-pointer items-center gap-2 rounded-xl border border-[var(--color-border)] bg-[var(--color-bg)] px-3 py-2 text-xs text-[var(--color-muted-text)] transition-colors hover:border-[var(--color-border-elevated)] sm:flex"
          aria-label="Open command palette"
        >
          <span aria-hidden="true">⌕</span>
          <span>Search or jump to…</span>
          <kbd className="ml-auto rounded border border-[var(--color-border)] px-1.5 py-0.5 font-mono text-[10px]">
            ⌘K
          </kbd>
        </button>
      </Dialog.Trigger>
      <Dialog.Portal>
        <Dialog.Overlay className="fixed inset-0 z-50 bg-black/30 backdrop-blur-[2px]" />
        <Dialog.Content className="fixed top-[14vh] left-1/2 z-50 w-[calc(100%-2rem)] max-w-xl -translate-x-1/2 overflow-hidden rounded-2xl border border-[var(--color-border)] bg-[var(--color-surface-elevated)] shadow-[var(--shadow-dialog)]">
          <Dialog.Title className="sr-only">Command palette</Dialog.Title>
          <label className="block border-b border-[var(--color-border)] p-4">
            <span className="sr-only">Search RunbookOS</span>
            <input
              autoFocus
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search screens and actions"
              className="w-full bg-transparent text-base outline-none placeholder:text-[var(--color-muted-text)]"
            />
          </label>
          <div className="max-h-[55vh] overflow-y-auto p-2">
            <p className="px-3 py-2 text-[11px] font-semibold tracking-wider text-[var(--color-muted-text)] uppercase">
              {query ? "Results" : "Recent items"}
            </p>
            {results.length ? (
              results.map(([label, href, copy]) => (
                <Dialog.Close asChild key={href}>
                  <Link
                    href={href}
                    className="block rounded-xl px-3 py-3 transition-colors hover:bg-[var(--color-hover)] focus:bg-[var(--color-hover)]"
                  >
                    <span className="block text-sm font-medium">{label}</span>
                    <span className="mt-0.5 block text-xs text-[var(--color-muted-text)]">
                      {copy}
                    </span>
                  </Link>
                </Dialog.Close>
              ))
            ) : (
              <p className="px-3 py-8 text-center text-sm text-[var(--color-muted-text)]">
                No matching destination.
              </p>
            )}
          </div>
          <div className="flex items-center justify-between border-t border-[var(--color-border)] px-4 py-3 text-[11px] text-[var(--color-muted-text)]">
            <span>Type to filter · Enter to open</span>
            <Dialog.Close className="cursor-pointer">Esc close</Dialog.Close>
          </div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
