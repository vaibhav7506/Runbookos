"use client";

import { useSyncExternalStore } from "react";
import { useTheme } from "@/components/theme-provider";
import { useQuery } from "@tanstack/react-query";
import { api } from "@/lib/api";
import { CommandPalette } from "@/components/command-palette";

interface TopbarProps {
  onToggleSidebar: () => void;
}

const subscribe = () => () => {};
const getClientSnapshot = () => true;
const getServerSnapshot = () => false;

export function Topbar({ onToggleSidebar }: TopbarProps) {
  const { setTheme, resolvedTheme } = useTheme();

  const mounted = useSyncExternalStore(
    subscribe,
    getClientSnapshot,
    getServerSnapshot
  );

  const currentUser = useQuery({
    queryKey: ["current-user"],
    queryFn: api.currentUser,
    retry: false,
  });

  const selected =
    currentUser.data?.organizations.find(
      (organization) =>
        organization.id === currentUser.data?.selectedOrganizationId
    ) ?? currentUser.data?.organizations[0];

  // Keep the remaining JSX exactly as it is.

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

        <CommandPalette />
      </div>

      {/* Right section */}
      <div className="flex items-center gap-2">
        <label className="flex cursor-pointer items-center gap-2 rounded-[var(--radius-md)] px-2.5 py-1.5 text-sm text-[var(--color-primary-text)] transition-colors duration-[var(--duration-fast)] hover:bg-[var(--color-hover)]">
          <div className="flex h-5 w-5 items-center justify-center rounded-[var(--radius-sm)] bg-[var(--color-control)]">
            <span className="text-[10px] font-bold text-[var(--color-control-text)]">
              {selected?.name.slice(0, 1).toUpperCase() ?? "R"}
            </span>
          </div>

          <span className="sr-only">Switch organization</span>

          <select
            aria-label="Switch organization"
            value={selected?.id ?? ""}
            onChange={async (event) => {
              await api.switchOrganization(event.target.value);
              window.location.reload();
            }}
            className="max-w-32 bg-transparent text-xs font-medium outline-none sm:max-w-48"
          >
            {!selected && <option value="">Workspace</option>}

            {currentUser.data?.organizations.map((organization) => (
              <option key={organization.id} value={organization.id}>
                {organization.name}
              </option>
            ))}
          </select>
        </label>

        {/* Theme toggle */}
        {!mounted ? (
          <button
            type="button"
            disabled
            aria-label="Toggle theme"
            className="rounded-[var(--radius-md)] p-2 text-[var(--color-secondary-text)]"
          >
            <span className="block h-4 w-4" aria-hidden="true" />
          </button>
        ) : (
          <button
            type="button"
            onClick={() => {
              const next = resolvedTheme === "light" ? "dark" : "light";
              setTheme(next);
            }}
            className="cursor-pointer rounded-[var(--radius-md)] p-2 text-[var(--color-secondary-text)] transition-colors duration-[var(--duration-fast)] hover:bg-[var(--color-hover)] hover:text-[var(--color-primary-text)]"
            aria-label={`Switch to ${
              resolvedTheme === "light" ? "dark" : "light"
            } theme`}
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
        )}

        {/* User avatar */}
        <button
          className="flex h-8 w-8 cursor-pointer items-center justify-center rounded-full bg-[var(--color-hover)] text-xs font-medium text-[var(--color-secondary-text)] transition-all duration-[var(--duration-fast)] hover:ring-2 hover:ring-[var(--color-border-elevated)]"
          aria-label="User menu"
        >
          {currentUser.data?.displayName.slice(0, 1).toUpperCase() ?? "U"}
        </button>
      </div>
    </header>
  );
}