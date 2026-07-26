"use client";

import { AppShell } from "@/components/shell";
import { SessionGuard } from "@/components/session-guard";

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  return (
    <SessionGuard>
      <AppShell>{children}</AppShell>
    </SessionGuard>
  );
}
