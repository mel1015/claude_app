"use client";

import { useState } from "react";
import { useTopVolume } from "@/hooks/useStocks";
import { LoadingSpinner } from "@/components/ui/LoadingSpinner";
import { ErrorMessage } from "@/components/ui/ErrorMessage";
import { StockTable } from "@/components/stocks/StockTable";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { TrendingUp } from "lucide-react";

export default function TopVolumePage() {
  const [market, setMarket] = useState("ALL");
  const { stocks, error, isLoading, mutate } = useTopVolume(market);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="flex items-center gap-2 text-2xl font-bold">
          <TrendingUp className="h-6 w-6" />
          거래량 TOP10
        </h1>
        <div className="flex gap-2">
          {[
            { key: "ALL", label: "전체" },
            { key: "KR", label: "🇰🇷 한국" },
            { key: "US", label: "🇺🇸 미국" },
          ].map(({ key, label }) => (
            <button
              key={key}
              onClick={() => setMarket(key)}
              className={`px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                market === key
                  ? "bg-primary text-primary-foreground"
                  : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
              }`}
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">
            {market === "ALL" ? "전체" : market === "KR" ? "🇰🇷 한국" : "🇺🇸 미국"} 거래량 상위 종목
          </CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {isLoading ? (
            <LoadingSpinner />
          ) : error ? (
            <ErrorMessage message="거래량 데이터를 불러올 수 없습니다" />
          ) : (
            <StockTable stocks={stocks || []} onFavoriteToggle={mutate} />
          )}
        </CardContent>
      </Card>
    </div>
  );
}
