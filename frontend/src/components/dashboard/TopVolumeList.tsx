"use client";

import Link from "next/link";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import type { StockDto } from "@/lib/types";
import { formatPrice, formatChangeRate, formatVolume, getChangeColor, cn } from "@/lib/utils";

interface TopVolumeListProps {
  stocks: StockDto[];
  title: string;
}

export function TopVolumeList({ stocks, title }: TopVolumeListProps) {
  return (
    <Card>
      <CardHeader className="pb-3">
        <CardTitle className="text-base">{title}</CardTitle>
      </CardHeader>
      <CardContent className="p-0">
        <div className="divide-y">
          {stocks?.map((stock, idx) => (
            <div
              key={`${stock.ticker}-${stock.market}`}
              className="flex items-center gap-3 px-4 py-2.5 hover:bg-muted/30"
            >
              <span className="text-xs font-bold text-muted-foreground w-5 text-center">
                {idx + 1}
              </span>
              <div className="flex-1 min-w-0">
                <Link
                  href={`/stocks/${stock.ticker}?market=${stock.market}`}
                  className="font-medium text-sm hover:text-primary truncate block"
                >
                  {stock.name}
                </Link>
                <div className="text-xs text-muted-foreground">{stock.ticker}</div>
              </div>
              <div className="text-right">
                <div className="text-sm font-mono">
                  {formatPrice(stock.closePrice, stock.market)}
                </div>
                <div className={cn("text-xs font-mono", getChangeColor(stock.changeRate))}>
                  {formatChangeRate(stock.changeRate)}
                </div>
              </div>
              <div className="text-xs text-muted-foreground font-mono w-16 text-right">
                {formatVolume(stock.volume)}
              </div>
            </div>
          ))}
          {(!stocks || stocks.length === 0) && (
            <div className="text-center py-8 text-muted-foreground text-sm">데이터 없음</div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
