"use client";

import {
  ResponsiveContainer,
  ComposedChart,
  Line,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ReferenceLine,
} from "recharts";
import type { StockDto } from "@/lib/types";
import { formatDate } from "@/lib/utils";

interface PriceChartProps {
  data: StockDto[];
  market?: string;
  referenceDates?: string[];
}

export function PriceChart({ data, market, referenceDates }: PriceChartProps) {
  if (!data || data.length === 0) {
    return <div className="text-center py-12 text-muted-foreground">차트 데이터가 없습니다</div>;
  }

  const calcMA = (prices: (number | null | undefined)[], period: number, idx: number) => {
    if (idx < period - 1) return null;
    const slice = prices.slice(idx - period + 1, idx + 1);
    if (slice.some((v) => v == null)) return null;
    return slice.reduce((sum, v) => sum! + v!, 0)! / period;
  };

  const sorted = [...data].reverse();
  const closes = sorted.map((d) => d.closePrice);

  const chartData = sorted.map((d, i) => ({
    date: d.tradeDate,
    close: d.closePrice,
    volume: d.volume,
    ma5: d.ma5 ?? calcMA(closes, 5, i),
    ma20: d.ma20 ?? calcMA(closes, 20, i),
    ma60: d.ma60 ?? calcMA(closes, 60, i),
  }));

  return (
    <div className="space-y-4">
      <ResponsiveContainer width="100%" height={300}>
        <ComposedChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
          <CartesianGrid strokeDasharray="3 3" className="stroke-border" />
          <XAxis
            dataKey="date"
            tick={{ fontSize: 11 }}
            tickFormatter={(v) => v?.slice(5) || ""}
          />
          <YAxis
            orientation="right"
            tick={{ fontSize: 11 }}
            tickFormatter={(v) => (v >= 1000 ? `${(v / 1000).toFixed(0)}k` : v)}
          />
          <Tooltip
            formatter={(value: number, name: string) => [value?.toFixed(2), name]}
            labelFormatter={(label) => formatDate(label)}
          />
          <Legend />
          <Line type="monotone" dataKey="close" stroke="#3b82f6" dot={false} strokeWidth={2} name="종가" />
          <Line type="monotone" dataKey="ma5" stroke="#f59e0b" dot={false} strokeWidth={1} name="MA5" />
          <Line type="monotone" dataKey="ma20" stroke="#10b981" dot={false} strokeWidth={1} name="MA20" />
          <Line type="monotone" dataKey="ma60" stroke="#8b5cf6" dot={false} strokeWidth={1} name="MA60" />
          {referenceDates?.map((date) => (
            <ReferenceLine key={date} x={date} stroke="#ef4444" strokeDasharray="4 2" label={{ value: "매칭", fontSize: 10, fill: "#ef4444" }} />
          ))}
        </ComposedChart>
      </ResponsiveContainer>

      <ResponsiveContainer width="100%" height={100}>
        <ComposedChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
          <CartesianGrid strokeDasharray="3 3" className="stroke-border" />
          <XAxis dataKey="date" tick={{ fontSize: 11 }} tickFormatter={(v) => v?.slice(5) || ""} />
          <YAxis orientation="right" tick={{ fontSize: 11 }} />
          <Tooltip labelFormatter={(label) => formatDate(label)} />
          <Bar dataKey="volume" fill="#94a3b8" name="거래량" />
        </ComposedChart>
      </ResponsiveContainer>
    </div>
  );
}
