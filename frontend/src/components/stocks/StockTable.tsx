"use client";

import React, { useState, useMemo, useEffect } from "react";
import Link from "next/link";
import { Star, StarOff, ChevronUp, ChevronDown, ChevronsUpDown } from "lucide-react";
import type { StockDto } from "@/lib/types";
import { formatPrice, formatChangeRate, formatVolume, getChangeColor, cn } from "@/lib/utils";
import { api } from "@/lib/api";

type SortKey = "name" | "market" | "closePrice" | "changeRate" | "volume" | "rsi14" | "ma20";
type SortDir = "asc" | "desc";

interface StockTableProps {
  stocks: StockDto[];
  showFavoriteButton?: boolean;
  onFavoriteToggle?: () => void;
  renderExtraColumn?: (stock: StockDto) => React.ReactNode;
}

function SortIcon({ col, sortKey, sortDir }: { col: SortKey; sortKey: SortKey | null; sortDir: SortDir }) {
  if (sortKey !== col) return <ChevronsUpDown className="inline h-3 w-3 ml-1 text-muted-foreground/50" />;
  return sortDir === "asc"
    ? <ChevronUp className="inline h-3 w-3 ml-1" />
    : <ChevronDown className="inline h-3 w-3 ml-1" />;
}

const toFavKeys = (stocks: StockDto[]) =>
  new Set(stocks.filter((s) => s.isFavorite).map((s) => `${s.ticker}|${s.market}`));

export function StockTable({ stocks, showFavoriteButton = true, onFavoriteToggle, renderExtraColumn }: StockTableProps) {
  const [favoriteLoading, setFavoriteLoading] = useState<string | null>(null);
  const [sortKey, setSortKey] = useState<SortKey | null>(null);
  const [sortDir, setSortDir] = useState<SortDir>("desc");
  const [favoriteKeys, setFavoriteKeys] = useState<Set<string>>(() => toFavKeys(stocks));

  useEffect(() => {
    setFavoriteKeys(toFavKeys(stocks));
  }, [stocks]);

  const handleSort = (key: SortKey) => {
    if (sortKey === key) {
      setSortDir((d) => (d === "asc" ? "desc" : "asc"));
    } else {
      setSortKey(key);
      setSortDir("desc");
    }
  };

  const sorted = useMemo(() => {
    if (!sortKey) return stocks;
    const getValue = (s: StockDto) => { const v = s[sortKey]; return v ?? (typeof v === "string" ? "" : -Infinity); };
    return [...stocks].sort((a, b) => {
      const av = getValue(a);
      const bv = getValue(b);
      if (av < bv) return sortDir === "asc" ? -1 : 1;
      if (av > bv) return sortDir === "asc" ? 1 : -1;
      return 0;
    });
  }, [stocks, sortKey, sortDir]);

  const handleFavorite = async (stock: StockDto) => {
    const loadingKey = `${stock.ticker}-${stock.market}`;
    const favKey = `${stock.ticker}|${stock.market}`;
    const isFav = favoriteKeys.has(favKey);
    setFavoriteLoading(loadingKey);
    // 낙관적 업데이트
    setFavoriteKeys((prev) => {
      const next = new Set(prev);
      isFav ? next.delete(favKey) : next.add(favKey);
      return next;
    });
    try {
      if (isFav) {
        await api.delete(`/api/v1/favorites/by-ticker?ticker=${stock.ticker}&market=${stock.market}`);
      } else {
        await api.post("/api/v1/favorites", {
          ticker: stock.ticker,
          market: stock.market,
          name: stock.name,
        });
      }
    } catch (err) {
      // 실패 시 롤백
      setFavoriteKeys((prev) => {
        const next = new Set(prev);
        isFav ? next.add(favKey) : next.delete(favKey);
        return next;
      });
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

  const th = (label: string, key: SortKey, align: "left" | "right" = "right") => (
    <th
      className={cn("px-4 py-3 font-medium cursor-pointer select-none hover:bg-muted/80 transition-colors", `text-${align}`)}
      onClick={() => handleSort(key)}
    >
      {label}
      <SortIcon col={key} sortKey={sortKey} sortDir={sortDir} />
    </th>
  );

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm">
        <thead>
          <tr className="border-b bg-muted/50">
            {th("종목", "name", "left")}
            {th("마켓", "market", "left")}
            {th("현재가", "closePrice")}
            {th("등락률", "changeRate")}
            {th("거래량", "volume")}
            {th("RSI", "rsi14")}
            {th("MA20", "ma20")}
            {renderExtraColumn && <th className="px-4 py-3 text-center font-medium hidden md:table-cell">차트</th>}
            {showFavoriteButton && <th className="px-4 py-3 text-center font-medium">관심</th>}
          </tr>
        </thead>
        <tbody>
          {sorted.map((stock) => (
            <tr key={`${stock.ticker}-${stock.market}`} className="border-b hover:bg-muted/30 transition-colors">
              <td className="px-4 py-3">
                <Link
                  href={`/stocks/${stock.ticker}?market=${stock.market}`}
                  className="font-medium text-primary hover:underline"
                >
                  {stock.name}
                </Link>
                <div className="text-xs text-muted-foreground">{stock.ticker}</div>
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
              {renderExtraColumn && (
                <td className="px-4 py-3 text-center hidden md:table-cell">
                  {renderExtraColumn(stock)}
                </td>
              )}
              {showFavoriteButton && (
                <td className="px-4 py-3 text-center">
                  <button
                    onClick={() => handleFavorite(stock)}
                    disabled={favoriteLoading === `${stock.ticker}-${stock.market}`}
                    className="p-1 rounded hover:bg-accent"
                    title="즐겨찾기 추가"
                  >
                    {favoriteKeys.has(`${stock.ticker}|${stock.market}`) ? (
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
