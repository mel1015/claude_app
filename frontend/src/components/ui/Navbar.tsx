"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { BarChart2, Star, TrendingUp, Bell, Newspaper } from "lucide-react";
import { cn } from "@/lib/utils";
import { useTheme } from "next-themes";
import { DataRefreshButton } from "./DataRefreshButton";

const navItems = [
  { href: "/", label: "대시보드", icon: BarChart2 },
  { href: "/favorites", label: "즐겨찾기", icon: Star },
  { href: "/top-volume", label: "거래량TOP", icon: TrendingUp },
  { href: "/signals", label: "시그널", icon: Bell },
  { href: "/news", label: "뉴스", icon: Newspaper },
];

export function Navbar() {
  const pathname = usePathname();
  const { theme, setTheme } = useTheme();

  return (
    <nav className="border-b bg-card">
      <div className="container mx-auto px-4">
        <div className="flex h-16 items-center justify-between">
          <div className="flex items-center gap-8">
            <Link href="/" className="text-xl font-bold text-primary">
              📈 Stock Report
            </Link>
            <div className="hidden md:flex items-center gap-1">
              {navItems.map(({ href, label, icon: Icon }) => (
                <Link
                  key={href}
                  href={href}
                  className={cn(
                    "flex items-center gap-2 px-3 py-2 rounded-md text-sm font-medium transition-colors",
                    pathname === href
                      ? "bg-primary text-primary-foreground"
                      : "text-muted-foreground hover:text-foreground hover:bg-accent"
                  )}
                >
                  <Icon className="h-4 w-4" />
                  {label}
                </Link>
              ))}
            </div>
          </div>
          <div className="flex items-center gap-2">
            <DataRefreshButton />
            <button
              onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
              className="p-2 rounded-md hover:bg-accent"
            >
              {theme === "dark" ? "☀️" : "🌙"}
            </button>
          </div>
        </div>
      </div>
    </nav>
  );
}
