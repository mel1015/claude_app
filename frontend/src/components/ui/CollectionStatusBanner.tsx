"use client";

import useSWR from "swr";
import { fetchApi } from "@/lib/api";

interface CollectionStatus {
  status: "IDLE" | "COLLECTING" | "COMPLETED" | "FAILED";
  message: string;
  collecting: boolean;
  lastUpdated: string;
}

export function CollectionStatusBanner() {
  const { data } = useSWR<CollectionStatus>(
    "/api/v1/system/collection-status",
    (url: string) => fetchApi<CollectionStatus>(url),
    { refreshInterval: (data) => (data?.collecting ? 3000 : 0) }
  );

  if (!data?.collecting) return null;

  return (
    <div className="bg-blue-50 dark:bg-blue-950 border-b border-blue-200 dark:border-blue-800">
      <div className="container mx-auto px-4 py-2 flex items-center gap-2 text-sm text-blue-700 dark:text-blue-300">
        <span className="inline-block h-2 w-2 rounded-full bg-blue-500 animate-pulse" />
        {data.message || "데이터 수집 중..."}
      </div>
    </div>
  );
}
