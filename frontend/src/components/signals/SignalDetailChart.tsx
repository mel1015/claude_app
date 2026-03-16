"use client";

import { useEffect } from "react";
import { X } from "lucide-react";
import { useStockHistory } from "@/hooks/useStocks";
import { PriceChart } from "@/components/stocks/PriceChart";
import { LoadingSpinner } from "@/components/ui/LoadingSpinner";
import type { StockDto } from "@/lib/types";

interface SignalDetailChartProps {
  stock: StockDto | null;
  onClose: () => void;
}

export function SignalDetailChart({ stock, onClose }: SignalDetailChartProps) {
  const { history, isLoading } = useStockHistory(stock?.market ?? "", stock?.ticker ?? "", 90);

  useEffect(() => {
    if (!stock) return;
    const handler = (e: KeyboardEvent) => { if (e.key === "Escape") onClose(); };
    document.addEventListener("keydown", handler);
    return () => document.removeEventListener("keydown", handler);
  }, [stock, onClose]);

  if (!stock) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 animate-in fade-in duration-150"
      onClick={(e) => e.target === e.currentTarget && onClose()}
    >
      <div className="w-full max-w-3xl max-h-[90vh] overflow-y-auto bg-card rounded-xl shadow-xl">
        <div className="flex items-center justify-between p-6 border-b">
          <div>
            <h2 className="text-lg font-bold">{stock.name}</h2>
            <p className="text-sm text-muted-foreground">
              {stock.ticker} · {stock.market} · 매칭일: {stock.tradeDate}
            </p>
          </div>
          <button onClick={onClose} className="p-2 rounded-md hover:bg-accent">
            <X className="h-5 w-5" />
          </button>
        </div>
        <div className="p-6">
          {isLoading ? (
            <div className="flex justify-center py-12"><LoadingSpinner /></div>
          ) : history && history.length > 0 ? (
            <PriceChart data={history} market={stock.market} referenceDates={[stock.tradeDate]} />
          ) : (
            <div className="text-center py-12 text-muted-foreground">차트 데이터가 없습니다</div>
          )}
        </div>
      </div>
    </div>
  );
}
