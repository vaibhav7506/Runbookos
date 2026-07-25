"use client";

import { useTheme } from "@/components/theme-provider";

interface TopbarProps {
  onToggleSidebar: () => void;
}

export function Topbar({ onToggleSidebar }: TopbarProps) {
  const { setTheme, resolvedTheme } = useTheme();

  return (
    <header className="flex h-[var(--topbar-height)] shrink-0 items-center justify-between border-b border-[var(--color-border)] bg-[var(--color-surface)] px-4">
      {/* Left section */}
      <div className="flex items-center gap-3">
        {/* Mobile menu toggle */}
        <button
          onClick={onToggleSidebar}
          className="cursor-pointer rounded-[var(--radius-sm)] p-1.5 transition-colors hover:bg-[var(--color-hover)] md:hidden"
          aria-label="Toggle navigation"
        >
          <svg
            width="18"
            height="18"
            viewBox="0 0 18 18"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.4"
            strokeLinecap="round"
            aria-hidden="true"
          >
            <path d="M3 5h12M3 9h12M3 13h12" />
          </svg>
        </button>

        {/* Command palette trigger (placeholder) */}
        <button
          className="hidden min-w-[200px] cursor-pointer items-center gap-2 rounded-[var(--radius-md)] border border-[var(--color-border)] bg-[var(--color-bg)] px-3 py-1.5 text-xs text-[var(--color-muted-text)] transition-colors duration-[var(--duration-fast)] hover:border-[var(--color-border-elevated)] sm:flex"
          aria-label="Open command palette"
        >
          <svg
            width="14"
            height="14"
            viewBox="0 0 14 14"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.3"
            strokeLinecap="round"
            aria-hidden="true"
          >
            <circle cx="6" cy="6" r="4.5" />
            <path d="M9.5 9.5L13 13" />
          </svg>
          <span>Search or jump to…</span>
          <kbd className="ml-auto rounded border border-[var(--color-border)] px-1 py-0.5 font-mono text-[10px] text-[var(--color-muted-text)]">
            ⌘K
          </kbd>
        </button>
      </div>

      {/* Right section */}
      <div className="flex items-center gap-2">
        {/* Organization switcher placeholder */}
        <button
          className="flex cursor-pointer items-center gap-2 rounded-[var(--radius-md)] px-2.5 py-1.5 text-sm text-[var(--color-primary-text)] transition-colors duration-[var(--duration-fast)] hover:bg-[var(--color-hover)]"
          aria-label="Switch organization"
        >
          <div className="flex h-5 w-5 items-center justify-center rounded-[var(--radius-sm)] bg-[var(--color-control)]">
            <span className="text-[10px] font-bold text-[var(--color-control-text)]">R</span>
          </div>
          <span className="hidden text-xs font-medium sm:inline">RunbookOS</span>
          <svg
            width="12"
            height="12"
            viewBox="0 0 12 12"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.3"
            strokeLinecap="round"
            aria-hidden="true"
          >
            <path d="M3 5l3 3 3-3" />
          </svg>
        </button>

        {/* Theme toggle */}
        <button
          onClick={() => {
            const next = resolvedTheme === "light" ? "dark" : "light";
            setTheme(next);
          }}
          className="cursor-pointer rounded-[var(--radius-md)] p-2 text-[var(--color-secondary-text)] transition-colors duration-[var(--duration-fast)] hover:bg-[var(--color-hover)] hover:text-[var(--color-primary-text)]"
          aria-label={`Switch to ${resolvedTheme === "light" ? "dark" : "light"} theme`}
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
              <path d="M8 1v1.5M8 13.5V15M1 8h1.5M13.5 8H15M3.05 3.05l1.06 1.06M11.89 11.89l1.06 1.06M3.05 12.95l1.06-1.06M11.89 4.11l1.06-1.06" />
              <circle cx="8" cy="8" r="3" />
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
              <path d="M13.5 8.5a5.5 5.5 0 01-6-6 5.5 5.5 0 106 6z" />
            </svg>
          )}
        </button>

        {/* User avatar placeholder */}
        <button
          className="flex h-8 w-8 cursor-pointer items-center justify-center rounded-full bg-[var(--color-hover)] text-xs font-medium text-[var(--color-secondary-text)] transition-all duration-[var(--duration-fast)] hover:ring-2 hover:ring-[var(--color-border-elevated)]"
          aria-label="User menu"
        >
          U
        </button>
      </div>
    </header>
  );
}
