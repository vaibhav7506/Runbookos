"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

interface SidebarProps {
  collapsed: boolean;
  onToggle: () => void;
}

interface NavItem {
  label: string;
  href: string;
  icon: React.ReactNode;
}

const navItems: NavItem[] = [
  {
    label: "Getting Started",
    href: "/dashboard/getting-started",
    icon: <GuideIcon />,
  },
  {
    label: "Overview",
    href: "/dashboard",
    icon: <OverviewIcon />,
  },
  {
    label: "Incidents",
    href: "/dashboard/incidents",
    icon: <IncidentsIcon />,
  },
  {
    label: "Runbooks",
    href: "/dashboard/runbooks",
    icon: <RunbooksIcon />,
  },
  {
    label: "Approvals",
    href: "/dashboard/approvals",
    icon: <ApprovalsIcon />,
  },
  {
    label: "Integrations",
    href: "/dashboard/integrations",
    icon: <IntegrationsIcon />,
  },
  {
    label: "Audit Log",
    href: "/dashboard/audit",
    icon: <AuditIcon />,
  },
  {
    label: "System Health",
    href: "/dashboard/operations",
    icon: <HealthIcon />,
  },
];

const bottomItems: NavItem[] = [
  {
    label: "Settings",
    href: "/dashboard/settings",
    icon: <SettingsIcon />,
  },
];

export function Sidebar({ collapsed, onToggle }: SidebarProps) {
  const pathname = usePathname();

  return (
    <aside
      className={`flex h-full shrink-0 flex-col border-r border-[var(--color-border)] bg-[var(--color-surface)] transition-[width] duration-[var(--duration-normal)] ease-[var(--ease-default)] ${collapsed ? "w-16" : "w-60"} `}
    >
      {/* Logo / Brand */}
      <div className="flex h-[var(--topbar-height)] items-center border-b border-[var(--color-border)] px-4">
        <Link href="/dashboard" className="flex min-w-0 items-center gap-2.5">
          <div className="flex h-7 w-7 shrink-0 items-center justify-center">
            <RunbookOSLogo />
          </div>
          {!collapsed && (
            <span className="truncate text-sm font-semibold text-[var(--color-primary-text)]">
              RunbookOS
            </span>
          )}
        </Link>
      </div>

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto px-2 py-2" aria-label="Main navigation">
        <ul className="flex flex-col gap-0.5">
          {navItems.map((item) => (
            <NavLink
              key={item.href}
              item={item}
              active={pathname === item.href || pathname.startsWith(item.href + "/")}
              collapsed={collapsed}
            />
          ))}
        </ul>
      </nav>

      {/* Bottom actions */}
      <div className="border-t border-[var(--color-border)] px-2 py-2">
        <ul className="flex flex-col gap-0.5">
          {bottomItems.map((item) => (
            <NavLink
              key={item.href}
              item={item}
              active={pathname === item.href}
              collapsed={collapsed}
            />
          ))}
          <li>
            <button
              onClick={onToggle}
              className={`flex w-full cursor-pointer items-center gap-2.5 rounded-[var(--radius-md)] px-2.5 py-2 text-sm text-[var(--color-secondary-text)] transition-colors duration-[var(--duration-fast)] hover:bg-[var(--color-hover)] hover:text-[var(--color-primary-text)]`}
              aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
            >
              <span className="flex h-5 w-5 shrink-0 items-center justify-center">
                <CollapseIcon collapsed={collapsed} />
              </span>
              {!collapsed && <span>Collapse</span>}
            </button>
          </li>
        </ul>
      </div>
    </aside>
  );
}

function NavLink({
  item,
  active,
  collapsed,
}: {
  item: NavItem;
  active: boolean;
  collapsed: boolean;
}) {
  return (
    <li>
      <Link
        href={item.href}
        className={`flex items-center gap-2.5 rounded-[var(--radius-md)] px-2.5 py-2 text-sm transition-colors duration-[var(--duration-fast)] ${
          active
            ? "bg-[var(--color-hover)] font-medium text-[var(--color-primary-text)]"
            : "text-[var(--color-secondary-text)] hover:bg-[var(--color-hover)] hover:text-[var(--color-primary-text)]"
        } `}
        aria-current={active ? "page" : undefined}
        title={collapsed ? item.label : undefined}
      >
        <span className="flex h-5 w-5 shrink-0 items-center justify-center">{item.icon}</span>
        {!collapsed && <span className="truncate">{item.label}</span>}
      </Link>
    </li>
  );
}

/* ---------- Icons (inline SVGs for zero dependency) ---------- */

function RunbookOSLogo() {
  return (
    <svg
      width="24"
      height="24"
      viewBox="0 0 24 24"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <rect x="3" y="3" width="18" height="18" rx="4" stroke="currentColor" strokeWidth="1.5" />
      <path
        d="M8 8h8M8 12h5M8 16h6"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinecap="round"
      />
      <circle cx="17" cy="15" r="2.5" stroke="currentColor" strokeWidth="1.5" />
      <path d="M19 17l1.5 1.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
    </svg>
  );
}

function OverviewIcon() {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 18 18"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.4"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <rect x="2" y="2" width="6" height="6" rx="1.5" />
      <rect x="10" y="2" width="6" height="6" rx="1.5" />
      <rect x="2" y="10" width="6" height="6" rx="1.5" />
      <rect x="10" y="10" width="6" height="6" rx="1.5" />
    </svg>
  );
}

function GuideIcon() {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 18 18"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.4"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M3 3.5h8.5A2.5 2.5 0 0114 6v8.5H5.5A2.5 2.5 0 013 12V3.5z" />
      <path d="M6 7h5M6 10h3" />
    </svg>
  );
}

function IncidentsIcon() {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 18 18"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.4"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M9 2L16 15H2L9 2z" />
      <path d="M9 7v3M9 12.5v.5" />
    </svg>
  );
}

function RunbooksIcon() {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 18 18"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.4"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <rect x="3" y="2" width="12" height="14" rx="2" />
      <path d="M6 6h6M6 9h4M6 12h5" />
    </svg>
  );
}

function ApprovalsIcon() {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 18 18"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.4"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <circle cx="9" cy="9" r="7" />
      <path d="M6 9l2 2 4-4" />
    </svg>
  );
}

function IntegrationsIcon() {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 18 18"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.4"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M7 3v4a2 2 0 01-2 2H3M11 3v4a2 2 0 002 2h2M7 15v-4a2 2 0 00-2-2H3M11 15v-4a2 2 0 012-2h2" />
    </svg>
  );
}

function AuditIcon() {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 18 18"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.4"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M9 2v14M2 9h14" />
      <circle cx="9" cy="9" r="7" />
    </svg>
  );
}

function SettingsIcon() {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 18 18"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.4"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <circle cx="9" cy="9" r="2.5" />
      <path d="M9 1.5v2M9 14.5v2M1.5 9h2M14.5 9h2M3.1 3.1l1.4 1.4M13.5 13.5l1.4 1.4M3.1 14.9l1.4-1.4M13.5 4.5l1.4-1.4" />
    </svg>
  );
}

function HealthIcon() {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 18 18"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.4"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M2 9h3l1.5-4 3 8 1.5-4H16" />
    </svg>
  );
}

function CollapseIcon({ collapsed }: { collapsed: boolean }) {
  return (
    <svg
      width="18"
      height="18"
      viewBox="0 0 18 18"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.4"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      className={`transition-transform duration-[var(--duration-normal)] ${collapsed ? "rotate-180" : ""}`}
    >
      <path d="M11 4L6 9l5 5" />
    </svg>
  );
}
