"use client";

import { useState, type ReactNode } from "react";
import { Sidebar } from "./sidebar";
import { Topbar } from "./topbar";

interface AppShellProps {
  children: ReactNode;
}

export function AppShell({ children }: AppShellProps) {
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <div className="flex h-screen overflow-hidden bg-[var(--color-bg)]">
      <div className="hidden md:block">
        <Sidebar
          collapsed={sidebarCollapsed}
          onToggle={() => setSidebarCollapsed(!sidebarCollapsed)}
        />
      </div>
      {mobileOpen && (
        <div className="fixed inset-0 z-40 flex md:hidden">
          <button
            aria-label="Close navigation"
            className="absolute inset-0 bg-black/35"
            onClick={() => setMobileOpen(false)}
          />
          <div className="relative z-10 h-full shadow-2xl">
            <Sidebar collapsed={false} onToggle={() => setMobileOpen(false)} />
          </div>
        </div>
      )}
      <div className="flex min-w-0 flex-1 flex-col">
        <Topbar onToggleSidebar={() => setMobileOpen(true)} />
        <main className="flex-1 overflow-auto">{children}</main>
      </div>
    </div>
  );
}
