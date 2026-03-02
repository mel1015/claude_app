"use client";

import { useState } from "react";
import Link from "next/link";
import { Star, StarOff } from "lucide-react";
import type { StockDto } from "@/lib/types";
import { formatPrice, formatChangeRate, formatVolume, getChangeColor, cn } from "@/lib/utils";
import { api } from "@/lib/api";

interface StockTableProps {
  stocks: StockDto[];
  showFavoriteButton?: boolean;
  onFavoriteToggle?: () => void;
}

export function StockTable({ stocks, showFavoriteButton = true, onFavoriteToggle }: StockTableProps) {
  const [favoriteLoading, setFavoriteLoading] = useState<string | null>(null);

  const handleFavorite = async (stock: StockDto) => {
    const key = `${stock.ticker}-${stock.market}`;
    setFavoriteLoading(key);
    try {
      await api.post("/api/v1/favorites", {
        ticker: stock.ticker,
        market: stock.market,
        name: stock.name,
      });
      onFavoriteToggle?.();
    } catch (err) {
      console.error("Failed to toggle favorite:", err);
    } finally {
      setFavoriteLoading(null);
    }
  };

  if (!stocks || stocks.length === 0) {
    return (
      <div className="text-center py-12 text-muted-foreground">
        종목 데이터가 없습니다
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b bg-muted/50">
            <th className="px-4 py-3 text-left font-medium">종목</th>
            <th className="px-4 py-3 text-left font-medium">마켓</th>
            <th className="px-4 py-3 text-right font-medium">현재가</th>
            <th className="px-4 py-3 text-right font-medium">등락률</th>
            <th className="px-4 py-3 text-right font-medium">거래량</th>
            <th className="px-4 py-3 text-right font-medium">RSI</th>
            <th className="px-4 py-3 text-right font-medium">MA20</th>
            {showFavoriteButton && <th className="px-4 py-3 text-center font-medium">관심</th>}
          </tr>
        </thead>
        <tbody>
          {stocks.map((stock) => (
            <tr key={`${stock.ticker}-${stock.market}`} className="border-b hover:bg-muted/30 transition-colors">
              <td className="px-4 py-3">
                <Link
                  href={`/stocks/${stock.ticker}?market=${stock.market}`}
                  className="font-medium text-primary hover:underline"
                >
                  {stock.ticker}
                </Link>
                <div className="text-xs text-muted-foreground">{stock.name}</div>
              </td>
              <td className="px-4 py-3">
                <span className="text-xs px-2 py-1 rounded bg-secondary text-secondary-foreground">
                  {stock.market}
                </span>
              </td>
              <td className="px-4 py-3 text-right font-mono">
                {formatPrice(stock.closePrice, stock.market)}
              </td>
              <td className={cn("px-4 py-3 text-right font-mono", getChangeColor(stock.changeRate))}>
                {formatChangeRate(stock.changeRate)}
              </td>
              <td className="px-4 py-3 text-right font-mono text-muted-foreground">
                {formatVolume(stock.volume)}
              </td>
              <td className="px-4 py-3 text-right font-mono">
                {stock.rsi14 != null ? (
                  <span className={cn(
                    stock.rsi14 > 70 ? "text-red-500" : stock.rsi14 < 30 ? "text-blue-500" : ""
                  )}>
                    {stock.rsi14.toFixed(1)}
                  </span>
                ) : "-"}
              </td>
              <td className="px-4 py-3 text-right font-mono text-muted-foreground">
                {stock.ma20 != null ? formatPrice(stock.ma20, stock.market) : "-"}
              </td>
              {showFavoriteButton && (
                <td className="px-4 py-3 text-center">
                  <button
                    onClick={() => handleFavorite(stock)}
                    disabled={favoriteLoading === `${stock.ticker}-${stock.market}`}
                    className="p-1 rounded hover:bg-accent"
                    title="즐겨찾기 추가"
                  >
                    {stock.isFavorite ? (
                      <Star className="h-4 w-4 fill-yellow-400 text-yellow-400" />
                    ) : (
                      <StarOff className="h-4 w-4 text-muted-foreground" />
                    )}
                  </button>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
