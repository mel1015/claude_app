"use client";

import { useState, useRef, useEffect } from "react";
import { RefreshCw, ChevronDown, Check, AlertCircle } from "lucide-react";
import { fetchApi } from "@/lib/api";
import { cn } from "@/lib/utils";

type RefreshType = "ALL" | "KR" | "US" | "NEWS";
type Status = "idle" | "loading" | "done" | "error";

const OPTIONS: { type: RefreshType; label: string; desc: string }[] = [
  { type: "ALL", label: "전체 수집", desc: "KR + US + 뉴스" },
  { type: "KR", label: "한국 주식", desc: "KOSPI / KOSDAQ" },
  { type: "US", label: "미국 주식", desc: "NYSE / NASDAQ" },
  { type: "NEWS", label: "뉴스", desc: "KR + US RSS" },
];

export function DataRefreshButton() {
  const [open, setOpen] = useState(false);
  const [status, setStatus] = useState<Status>("idle");
  const [activeType, setActiveType] = useState<RefreshType | null>(null);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, []);

  async function handleRefresh(type: RefreshType) {
    setOpen(false);
    setActiveType(type);
    setStatus("loading");
    try {
      const url =
        type === "ALL"
          ? "/api/v1/system/refresh-cache"
          : `/api/v1/system/refresh-cache?type=${type}`;
      await fetchApi(url, { method: "POST" });
      setStatus("done");
    } catch {
      setStatus("error");
    } finally {
      setTimeout(() => {
        setStatus("idle");
        setActiveType(null);
      }, 3000);
    }
  }

  const isLoading = status === "loading";

  return (
    <div className="relative" ref={ref}>
      <button
        onClick={() => !isLoading && setOpen((v) => !v)}
        disabled={isLoading}
        className={cn(
          "flex items-center gap-1.5 px-3 py-1.5 rounded-md text-sm font-medium transition-colors",
          isLoading
            ? "bg-primary/20 text-primary cursor-not-allowed"
            : status === "done"
            ? "bg-green-500/20 text-green-600 dark:text-green-400"
            : status === "error"
            ? "bg-red-500/20 text-red-600 dark:text-red-400"
            : "hover:bg-accent text-muted-foreground hover:text-foreground"
        )}
      >
        {status === "done" ? (
          <Check className="h-4 w-4" />
        ) : status === "error" ? (
          <AlertCircle className="h-4 w-4" />
        ) : (
          <RefreshCw className={cn("h-4 w-4", isLoading && "animate-spin")} />
        )}
        <span className="hidden sm:inline">
          {isLoading
            ? `${OPTIONS.find((o) => o.type === activeType)?.label ?? ""} 수집중…`
            : status === "done"
            ? "수집 시작됨"
            : status === "error"
            ? "오류 발생"
            : "데이터 수집"}
        </span>
        {status === "idle" && <ChevronDown className="h-3 w-3" />}
      </button>

      {open && (
        <div className="absolute right-0 mt-1 w-44 rounded-md border bg-card shadow-md z-50">
          {OPTIONS.map(({ type, label, desc }) => (
            <button
              key={type}
              onClick={() => handleRefresh(type)}
              className="w-full flex flex-col items-start px-3 py-2 text-left hover:bg-accent transition-colors first:rounded-t-md last:rounded-b-md"
            >
              <span className="text-sm font-medium">{label}</span>
              <span className="text-xs text-muted-foreground">{desc}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
