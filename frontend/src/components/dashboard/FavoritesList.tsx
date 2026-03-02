"use client";

import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import type { FavoriteDto } from "@/lib/types";
import { formatPrice, formatChangeRate, getChangeColor, cn } from "@/lib/utils";
import { Star, Trash2 } from "lucide-react";
import { api } from "@/lib/api";

interface FavoritesListProps {
  favorites: FavoriteDto[];
  onUpdate?: () => void;
}

export function FavoritesList({ favorites, onUpdate }: FavoritesListProps) {
  const handleRemove = async (id: string) => {
    try {
      await api.delete(`/api/v1/favorites/${id}`);
      onUpdate?.();
    } catch (err) {
      console.error("Failed to remove favorite:", err);
    }
  };

  return (
    <Card>
      <CardHeader className="pb-3">
        <CardTitle className="flex items-center gap-2 text-base">
          <Star className="h-4 w-4 fill-yellow-400 text-yellow-400" />
          즐겨찾기
        </CardTitle>
      </CardHeader>
      <CardContent className="p-0">
        <div className="divide-y">
          {favorites?.map((fav) => (
            <div key={fav.id} className="flex items-center gap-3 px-4 py-2.5 hover:bg-muted/30">
              <div className="flex-1 min-w-0">
                <Link
                  href={`/stocks/${fav.ticker}?market=${fav.market}`}
                  className="font-medium text-sm hover:text-primary"
                >
                  {fav.ticker}
                </Link>
                <div className="text-xs text-muted-foreground">{fav.name}</div>
              </div>
              {fav.latestStock && (
                <div className="text-right">
                  <div className="text-sm font-mono">
                    {formatPrice(fav.latestStock.closePrice, fav.market)}
                  </div>
                  <div className={cn("text-xs font-mono", getChangeColor(fav.latestStock.changeRate))}>
                    {formatChangeRate(fav.latestStock.changeRate)}
                  </div>
                </div>
              )}
              <button
                onClick={() => handleRemove(fav.id)}
                className="p-1 rounded hover:bg-destructive/10 text-muted-foreground hover:text-destructive"
              >
                <Trash2 className="h-3.5 w-3.5" />
              </button>
            </div>
          ))}
          {(!favorites || favorites.length === 0) && (
            <div className="text-center py-8 text-muted-foreground text-sm">즐겨찾기가 없습니다</div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
