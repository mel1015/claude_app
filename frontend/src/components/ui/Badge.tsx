import { cn } from "@/lib/utils";

interface BadgeProps {
  children: React.ReactNode;
  variant?: "default" | "secondary" | "outline" | "destructive" | "success";
  className?: string;
}

export function Badge({ children, variant = "default", className }: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-semibold",
        {
          "bg-primary text-primary-foreground": variant === "default",
          "bg-secondary text-secondary-foreground": variant === "secondary",
          "border border-input": variant === "outline",
          "bg-destructive/10 text-destructive": variant === "destructive",
          "bg-green-100 text-green-700 dark:bg-green-900/20 dark:text-green-400": variant === "success",
        },
        className
      )}
    >
      {children}
    </span>
  );
}
