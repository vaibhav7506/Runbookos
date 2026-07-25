type BadgeVariant = "critical" | "warning" | "success" | "info" | "neutral";

interface StatusBadgeProps {
  variant?: BadgeVariant;
  children: React.ReactNode;
  className?: string;
  dot?: boolean;
}

const variantStyles: Record<BadgeVariant, string> = {
  critical: "bg-[var(--color-critical-bg)] text-[var(--color-critical)]",
  warning: "bg-[var(--color-warning-bg)] text-[var(--color-warning)]",
  success: "bg-[var(--color-success-bg)] text-[var(--color-success)]",
  info: "bg-[var(--color-info-bg)] text-[var(--color-info)]",
  neutral: "bg-[var(--color-hover)] text-[var(--color-secondary-text)]",
};

const dotColors: Record<BadgeVariant, string> = {
  critical: "bg-[var(--color-critical)]",
  warning: "bg-[var(--color-warning)]",
  success: "bg-[var(--color-success)]",
  info: "bg-[var(--color-info)]",
  neutral: "bg-[var(--color-muted-text)]",
};

export function StatusBadge({
  variant = "neutral",
  children,
  className = "",
  dot = false,
}: StatusBadgeProps) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-[var(--radius-full)] px-2 py-0.5 text-xs font-medium ${variantStyles[variant]} ${className} `.trim()}
    >
      {dot && (
        <span
          className={`inline-block h-1.5 w-1.5 rounded-full ${dotColors[variant]}`}
          aria-hidden="true"
        />
      )}
      {children}
    </span>
  );
}
