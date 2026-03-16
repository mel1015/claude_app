"use client";

import { useEffect, useRef, useState } from "react";
import useSWR from "swr";
import { LineChart, Line, ReferenceLine, ResponsiveContainer } from "recharts";
import { fetchApi } from "@/lib/api";
import type { ApiResponse, StockDto } from "@/lib/types";

interface SignalMiniChartProps {
  market: string;
  ticker: string;
  tradeDate: string;
  onChartClick?: () => void;
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const fetcher = (url: string) => fetchApi<any>(url);

export function SignalMiniChart({ market, ticker, tradeDate, onChartClick }: SignalMiniChartProps) {
  const ref = useRef<HTMLDivElement>(null);
  const [isVisible, setIsVisible] = useState(false);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const observer = new IntersectionObserver(
      ([entry]) => { if (entry.isIntersecting) setIsVisible(true); },
      { threshold: 0.1 }
    );
    observer.observe(el);
    return () => observer.disconnect();
  }, []);

  const url = market && ticker && isVisible
    ? `/api/v1/stocks/${market}/${ticker}/history?days=90`
    : null;

  const { data, isLoading, error } = useSWR<ApiResponse<StockDto[]>>(url, fetcher);

  const chartData = data?.data
    ? [...data.data].reverse().map((d) => ({ date: d.tradeDate, close: d.closePrice }))
    : [];

  return (
    <div
      ref={ref}
      className="hidden md:block w-[120px] h-[40px] cursor-pointer"
      onClick={(e) => { e.stopPropagation(); onChartClick?.(); }}
      title="차트 보기"
    >
      {!isVisible || isLoading ? (
        <div className="w-full h-full rounded bg-muted animate-pulse" />
      ) : error || chartData.length === 0 ? (
        <div className="w-full h-full flex items-center justify-center text-xs text-muted-foreground">-</div>
      ) : (
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={chartData} margin={{ top: 2, right: 2, left: 2, bottom: 2 }}>
            <Line type="monotone" dataKey="close" stroke="#3b82f6" dot={false} strokeWidth={1.5} />
            <ReferenceLine x={tradeDate} stroke="#ef4444" strokeWidth={1.5} />
          </LineChart>
        </ResponsiveContainer>
      )}
    </div>
  );
}
