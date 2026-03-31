"use client";

import { useState } from "react";
import { useParams, useSearchParams } from "next/navigation";
import { useStock, useStockHistory, useFavorites } from "@/hooks/useStocks";
import { LoadingSpinner } from "@/components/ui/LoadingSpinner";
import { ErrorMessage } from "@/components/ui/ErrorMessage";
import { PriceChart } from "@/components/stocks/PriceChart";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import {
  formatPrice,
  formatChangeRate,
  formatVolume,
  getChangeColor,
  formatNumber,
  cn,
} from "@/lib/utils";
import { Star, StarOff } from "lucide-react";
import { api } from "@/lib/api";

export default function StockDetailPage() {
  const params = useParams();
  const searchParams = useSearchParams();
  const ticker = params.ticker as string;
  const market = searchParams.get("market") || "KOSPI";

  const [days, setDays] = useState(90);
  const { stock, error, isLoading } = useStock(market, ticker);
  const { history } = useStockHistory(market, ticker, days);
  const { favorites, mutate: mutateFavorites } = useFavorites();

  const favItem = favorites?.find((f) => f.ticker === ticker && f.market === market);
  const isFavorite = !!favItem;

  const handleToggleFavorite = async () => {
    if (isFavorite && favItem) {
      await api.delete(`/api/v1/favorites/${favItem.id}`);
    } else {
      await api.post("/api/v1/favorites", { ticker, market, name: stock?.name });
    }
    mutateFavorites();
  };

  if (isLoading) return <LoadingSpinner />;
  if (error) return <ErrorMessage message={`종목을 찾을 수 없습니다: ${ticker}`} />;
  if (!stock) return null;

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="text-3xl font-bold">{stock.name}</h1>
            <Badge variant="secondary">{stock.market}</Badge>
          </div>
          <div className="text-muted-foreground">{stock.ticker}</div>
        </div>
        <button
          onClick={handleToggleFavorite}
          className="flex items-center gap-2 px-4 py-2 rounded-md border hover:bg-accent transition-colors"
        >
          {isFavorite ? (
            <>
              <Star className="h-4 w-4 fill-yellow-400 text-yellow-400" /> 즐겨찾기 삭제
            </>
          ) : (
            <>
              <StarOff className="h-4 w-4" /> 즐겨찾기 추가
            </>
          )}
        </button>
      </div>

      {/* Price info */}
      <Card>
        <CardContent className="pt-6">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
            <div>
              <div className="text-sm text-muted-foreground">현재가</div>
              <div className="text-3xl font-bold font-mono mt-1">
                {formatPrice(stock.closePrice, stock.market)}
              </div>
              <div className={cn("text-lg font-mono mt-1", getChangeColor(stock.changeRate))}>
                {formatChangeRate(stock.changeRate)}
              </div>
            </div>
            <div className="space-y-2">
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">시가</span>
                <span className="font-mono">{formatPrice(stock.openPrice, stock.market)}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">고가</span>
                <span className="font-mono text-red-500">
                  {formatPrice(stock.highPrice, stock.market)}
                </span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">저가</span>
                <span className="font-mono text-blue-500">
                  {formatPrice(stock.lowPrice, stock.market)}
                </span>
              </div>
            </div>
            <div className="space-y-2">
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">거래량</span>
                <span className="font-mono">{formatVolume(stock.volume)}</span>
              </div>
              {stock.marketCap && (
                <div className="flex justify-between text-sm">
                  <span className="text-muted-foreground">시가총액</span>
                  <span className="font-mono">{formatVolume(stock.marketCap)}</span>
                </div>
              )}
            </div>
            <div className="space-y-2">
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">RSI(14)</span>
                <span
                  className={cn(
                    "font-mono",
                    stock.rsi14 && stock.rsi14 > 70
                      ? "text-red-500"
                      : stock.rsi14 && stock.rsi14 < 30
                      ? "text-blue-500"
                      : ""
                  )}
                >
                  {formatNumber(stock.rsi14, 1)}
                </span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">MA20</span>
                <span className="font-mono">{formatPrice(stock.ma20, stock.market)}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-muted-foreground">MACD</span>
                <span
                  className={cn(
                    "font-mono",
                    stock.macdHist && stock.macdHist > 0 ? "text-red-500" : "text-blue-500"
                  )}
                >
                  {formatNumber(stock.macd)}
                </span>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Chart */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="text-base">주가 차트</CardTitle>
            <div className="flex gap-2">
              {[30, 60, 90].map((d) => (
                <button
                  key={d}
                  onClick={() => setDays(d)}
                  className={`px-3 py-1 text-xs rounded ${
                    days === d
                      ? "bg-primary text-primary-foreground"
                      : "bg-secondary text-secondary-foreground hover:bg-secondary/80"
                  }`}
                >
                  {d}일
                </button>
              ))}
            </div>
          </div>
        </CardHeader>
        <CardContent>
          <PriceChart data={history || []} market={stock.market} />
        </CardContent>
      </Card>

      {/* Technical Indicators */}
      <Card>
        <CardHeader>
          <CardTitle className="text-base">기술적 지표</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {[
              { label: "MA5", value: formatPrice(stock.ma5, stock.market) },
              { label: "MA20", value: formatPrice(stock.ma20, stock.market) },
              { label: "MA60", value: formatPrice(stock.ma60, stock.market) },
              { label: "RSI(14)", value: formatNumber(stock.rsi14, 1) },
              { label: "MACD", value: formatNumber(stock.macd) },
              { label: "Signal", value: formatNumber(stock.macdSignal) },
              { label: "Hist", value: formatNumber(stock.macdHist) },
            ].map(({ label, value }) => (
              <div key={label} className="text-center p-3 rounded-lg bg-muted/30">
                <div className="text-xs text-muted-foreground">{label}</div>
                <div className="font-mono font-medium mt-1">{value}</div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
