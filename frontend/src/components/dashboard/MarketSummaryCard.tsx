"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import type { MarketSummary } from "@/lib/types";
import { formatChangeRate, getChangeColor, cn } from "@/lib/utils";
import { TrendingUp, TrendingDown } from "lucide-react";

interface MarketSummaryCardProps {
  summary: MarketSummary;
  label: string;
}

export function MarketSummaryCard({ summary, label }: MarketSummaryCardProps) {
  const risingPct =
    summary.totalStocks > 0
      ? ((summary.risingStocks / summary.totalStocks) * 100).toFixed(1)
      : "0";

  return (
    <Card>
      <CardHeader className="pb-2">
        <CardTitle className="text-base">{label} 시장 요약</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-1">
            <div className="text-xs text-muted-foreground">평균 등락률</div>
            <div className={cn("text-xl font-bold", getChangeColor(summary.avgChangeRate))}>
              {formatChangeRate(summary.avgChangeRate)}
            </div>
          </div>
          <div className="space-y-1">
            <div className="text-xs text-muted-foreground">총 종목수</div>
            <div className="text-xl font-bold">{summary.totalStocks.toLocaleString()}</div>
          </div>
          <div className="flex items-center gap-1">
            <TrendingUp className="h-4 w-4 text-red-500" />
            <span className="text-sm text-red-500">{summary.risingStocks}</span>
          </div>
          <div className="flex items-center gap-1">
            <TrendingDown className="h-4 w-4 text-blue-500" />
            <span className="text-sm text-blue-500">{summary.fallingStocks}</span>
          </div>
        </div>
        <div className="mt-3">
          <div className="text-xs text-muted-foreground mb-1">상승 비율</div>
          <div className="w-full bg-secondary rounded-full h-2">
            <div
              className="bg-red-500 h-2 rounded-full transition-all"
              style={{ width: `${risingPct}%` }}
            />
          </div>
          <div className="text-xs text-muted-foreground mt-1">{risingPct}% 상승</div>
        </div>
      </CardContent>
    </Card>
  );
}
