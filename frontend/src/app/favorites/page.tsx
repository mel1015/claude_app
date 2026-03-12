"use client";

import { useFavorites } from "@/hooks/useStocks";
import { LoadingSpinner } from "@/components/ui/LoadingSpinner";
import { ErrorMessage } from "@/components/ui/ErrorMessage";
import { Card, CardContent } from "@/components/ui/Card";
import Link from "next/link";
import { Star, Trash2, TrendingUp, TrendingDown } from "lucide-react";
import { formatPrice, formatChangeRate, getChangeColor, cn } from "@/lib/utils";
import { api } from "@/lib/api";

export default function FavoritesPage() {
  const { favorites, error, isLoading, mutate } = useFavorites();

  const handleRemove = async (id: string) => {
    try {
      await api.delete(`/api/v1/favorites/${id}`);
      mutate();
    } catch (err) {
      console.error("Remove failed:", err);
    }
  };

  if (isLoading) return <LoadingSpinner />;
  if (error) return <ErrorMessage message="즐겨찾기를 불러올 수 없습니다" />;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="flex items-center gap-2 text-2xl font-bold">
          <Star className="h-6 w-6 fill-yellow-400 text-yellow-400" />
          즐겨찾기
        </h1>
        <span className="text-sm text-muted-foreground">{favorites?.length || 0}개</span>
      </div>

      {!favorites || favorites.length === 0 ? (
        <Card>
          <CardContent className="py-16 text-center">
            <Star className="h-12 w-12 mx-auto text-muted-foreground mb-4" />
            <p className="text-muted-foreground">즐겨찾기가 없습니다</p>
            <p className="text-sm text-muted-foreground mt-1">
              <Link href="/top-volume" className="text-primary hover:underline">
                거래량 TOP10
              </Link>
              에서 종목을 추가하세요
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {favorites.map((fav) => (
            <Card key={fav.id} className="hover:shadow-md transition-shadow">
              <CardContent className="p-4">
                <div className="flex items-start justify-between mb-2">
                  <Link href={`/stocks/${fav.ticker}?market=${fav.market}`}>
                    <div className="font-bold text-lg hover:text-primary">{fav.name}</div>
                    <div className="text-xs text-muted-foreground">{fav.ticker}</div>
                    <div className="text-xs text-muted-foreground">{fav.market}</div>
                  </Link>
                  <button
                    onClick={() => handleRemove(fav.id)}
                    className="p-1 hover:bg-destructive/10 rounded text-muted-foreground hover:text-destructive"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                </div>
                {fav.latestStock ? (
                  <div>
                    <div className="text-xl font-mono font-bold mt-3">
                      {formatPrice(fav.latestStock.closePrice, fav.market)}
                    </div>
                    <div
                      className={cn(
                        "flex items-center gap-1 text-sm font-mono",
                        getChangeColor(fav.latestStock.changeRate)
                      )}
                    >
                      {fav.latestStock.changeRate && fav.latestStock.changeRate > 0 ? (
                        <TrendingUp className="h-4 w-4" />
                      ) : (
                        <TrendingDown className="h-4 w-4" />
                      )}
                      {formatChangeRate(fav.latestStock.changeRate)}
                    </div>
                    {fav.latestStock.rsi14 != null && (
                      <div className="mt-2 text-xs text-muted-foreground">
                        RSI:{" "}
                        <span
                          className={cn(
                            fav.latestStock.rsi14 > 70
                              ? "text-red-500"
                              : fav.latestStock.rsi14 < 30
                              ? "text-blue-500"
                              : ""
                          )}
                        >
                          {fav.latestStock.rsi14.toFixed(1)}
                        </span>
                      </div>
                    )}
                  </div>
                ) : (
                  <div className="text-sm text-muted-foreground mt-3">가격 정보 없음</div>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
