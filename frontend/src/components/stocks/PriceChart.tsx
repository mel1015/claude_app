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

interface CandlestickBarProps {
  x?: number;
  y?: number;
  width?: number;
  height?: number;
  payload?: {
    openPrice: number;
    closePrice: number;
    highPrice: number;
    lowPrice: number;
  };
}

const CandlestickBar = (props: CandlestickBarProps) => {
  const { x = 0, y = 0, width = 0, height = 0, payload } = props;
  if (!payload) return null;

  const { openPrice, closePrice, highPrice, lowPrice } = payload;
  const range = highPrice - lowPrice;
  if (range === 0) return null;

  const isUp = closePrice >= openPrice;
  const color = isUp ? "#ef4444" : "#3b82f6";

  // y = pixel for highPrice, y+height = pixel for lowPrice (recharts range bar 기준)
  const yOpen = y + height * (highPrice - openPrice) / range;
  const yClose = y + height * (highPrice - closePrice) / range;

  const bodyTop = Math.min(yOpen, yClose);
  const bodyHeight = Math.max(Math.abs(yClose - yOpen), 1);
  const centerX = x + width / 2;
  const bodyWidth = Math.max(width - 2, 2);

  return (
    <g>
      <line x1={centerX} y1={y} x2={centerX} y2={y + height} stroke={color} strokeWidth={1} />
      <rect x={x + 1} y={bodyTop} width={bodyWidth} height={bodyHeight} fill={color} stroke={color} />
    </g>
  );
};

interface ChartDataPoint {
  date: string;
  openPrice: number | null | undefined;
  closePrice: number | null | undefined;
  highPrice: number | null | undefined;
  lowPrice: number | null | undefined;
  changeRate: number | null | undefined;
  volume: number | null | undefined;
  ma5: number | null;
  ma20: number | null;
  ma60: number | null;
}

interface CandlestickTooltipProps {
  active?: boolean;
  payload?: { payload: ChartDataPoint }[];
  label?: string;
}

const CandlestickTooltip = ({ active, payload, label }: CandlestickTooltipProps) => {
  if (!active || !payload?.length) return null;
  const d = payload[0]?.payload;
  if (!d) return null;
  const isUp = (d.closePrice ?? 0) >= (d.openPrice ?? 0);
  const priceColor = isUp ? "#ef4444" : "#3b82f6";
  return (
    <div className="bg-background border border-border rounded p-2 text-xs space-y-1 shadow-md">
      <div className="font-medium">{formatDate(label)}</div>
      <div>시가: {d.openPrice?.toFixed(2)}</div>
      <div style={{ color: "#ef4444" }}>고가: {d.highPrice?.toFixed(2)}</div>
      <div style={{ color: "#3b82f6" }}>저가: {d.lowPrice?.toFixed(2)}</div>
      <div style={{ color: priceColor }}>종가: {d.closePrice?.toFixed(2)}</div>
      {d.changeRate != null && (
        <div style={{ color: priceColor }}>
          등락률: {d.changeRate >= 0 ? "+" : ""}{d.changeRate?.toFixed(2)}%
        </div>
      )}
      {d.ma5 != null && <div style={{ color: "#f59e0b" }}>MA5: {d.ma5?.toFixed(2)}</div>}
      {d.ma20 != null && <div style={{ color: "#10b981" }}>MA20: {d.ma20?.toFixed(2)}</div>}
      {d.ma60 != null && <div style={{ color: "#8b5cf6" }}>MA60: {d.ma60?.toFixed(2)}</div>}
    </div>
  );
};

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
    openPrice: d.openPrice,
    highPrice: d.highPrice,
    lowPrice: d.lowPrice,
    closePrice: d.closePrice,
    candle: [d.lowPrice, d.highPrice] as [number, number],
    changeRate: d.changeRate,
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
            domain={["auto", "auto"]}
          />
          <Tooltip content={<CandlestickTooltip />} />
          <Legend />
          <Bar dataKey="candle" name="캔들" shape={<CandlestickBar />} isAnimationActive={false} legendType="none" />
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
