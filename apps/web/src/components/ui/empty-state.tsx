import type { ReactNode } from "react";

interface EmptyStateProps {
  icon?: ReactNode;
  title: string;
  description?: string;
  action?: ReactNode;
  className?: string;
}

export function EmptyState({ icon, title, description, action, className = "" }: EmptyStateProps) {
  return (
    <div
      className={`flex flex-col items-center justify-center px-6 py-16 text-center ${className} `.trim()}
    >
      {icon && <div className="mb-4 text-[var(--color-muted-text)]">{icon}</div>}
      <h3 className="mb-1 text-sm font-semibold text-[var(--color-primary-text)]">{title}</h3>
      {description && (
        <p className="mb-4 max-w-sm text-sm text-[var(--color-secondary-text)]">{description}</p>
      )}
      {action && <div>{action}</div>}
    </div>
  );
}
