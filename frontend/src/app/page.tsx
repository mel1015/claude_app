"use client";

import { useDashboard, useFavorites } from "@/hooks/useStocks";
import { LoadingSpinner } from "@/components/ui/LoadingSpinner";
import { ErrorMessage } from "@/components/ui/ErrorMessage";
import { MarketSummaryCard } from "@/components/dashboard/MarketSummaryCard";
import { TopVolumeList } from "@/components/dashboard/TopVolumeList";
import { FavoritesList } from "@/components/dashboard/FavoritesList";
import { NewsWidget } from "@/components/dashboard/NewsWidget";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { formatDate } from "@/lib/utils";
import Link from "next/link";
import { Bell } from "lucide-react";

export default function DashboardPage() {
  const { dashboard, error, isLoading, mutate } = useDashboard();
  const { favorites, mutate: mutateFavs } = useFavorites();

  if (isLoading) return <LoadingSpinner />;
  if (error) return <ErrorMessage message="대시보드 데이터를 불러올 수 없습니다" />;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">일일 주식 리포트</h1>
        <span className="text-sm text-muted-foreground">
          기준일: {formatDate(dashboard?.tradeDate)}
        </span>
      </div>

      {/* Market Summaries */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {dashboard?.krSummary && (
          <MarketSummaryCard summary={dashboard.krSummary} label="🇰🇷 한국" />
        )}
        {dashboard?.usSummary && (
          <MarketSummaryCard summary={dashboard.usSummary} label="🇺🇸 미국" />
        )}
      </div>

      {/* Main grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <TopVolumeList stocks={dashboard?.topVolumeKr || []} title="🇰🇷 한국 거래량 TOP10" />
        <TopVolumeList stocks={dashboard?.topVolumeUs || []} title="🇺🇸 미국 거래량 TOP10" />

        <div className="space-y-4">
          <FavoritesList favorites={favorites || []} onUpdate={() => { mutate(); mutateFavs(); }} />

          {dashboard?.activeSignals && dashboard.activeSignals.length > 0 && (
            <Card>
              <CardHeader className="pb-3">
                <CardTitle className="flex items-center gap-2 text-base">
                  <Bell className="h-4 w-4" />
                  활성 시그널
                </CardTitle>
              </CardHeader>
              <CardContent className="p-0">
                <div className="divide-y">
                  {dashboard.activeSignals.map((signal) => (
                    <div key={signal.id} className="px-4 py-2.5 hover:bg-muted/30">
                      <Link href="/signals" className="text-sm font-medium hover:text-primary">
                        {signal.name}
                      </Link>
                      <div className="text-xs text-muted-foreground">{signal.marketFilter}</div>
                    </div>
                  ))}
                </div>
              </CardContent>
            </Card>
          )}
        </div>
      </div>

      {/* News */}
      <NewsWidget news={dashboard?.latestNews || []} />
    </div>
  );
}
