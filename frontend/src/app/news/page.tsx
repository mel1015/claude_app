"use client";

import { useState } from "react";
import { useNews } from "@/hooks/useStocks";
import { LoadingSpinner } from "@/components/ui/LoadingSpinner";
import { ErrorMessage } from "@/components/ui/ErrorMessage";
import { Card, CardContent } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Newspaper, ExternalLink, ChevronLeft, ChevronRight } from "lucide-react";
import { formatDate } from "@/lib/utils";

export default function NewsPage() {
  const [market, setMarket] = useState("ALL");
  const [page, setPage] = useState(0);
  const { news, meta, error, isLoading } = useNews(market, undefined, page, 20);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="flex items-center gap-2 text-2xl font-bold">
          <Newspaper className="h-6 w-6" />
          주식 뉴스
        </h1>
        <div className="flex gap-2">
          {[
            { key: "ALL", label: "전체" },
            { key: "KR", label: "🇰🇷 한국" },
            { key: "US", label: "🇺🇸 미국" },
          ].map(({ key, label }) => (
            <button
              key={key}
              onClick={() => {
                setMarket(key);
                setPage(0);
              }}
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

      {isLoading ? (
        <LoadingSpinner />
      ) : error ? (
        <ErrorMessage message="뉴스를 불러올 수 없습니다" />
      ) : (
        <>
          <div className="space-y-3">
            {news?.map((item) => (
              <Card key={item.id} className="hover:shadow-md transition-shadow">
                <CardContent className="p-4">
                  <div className="flex items-start gap-3">
                    <Badge
                      variant={item.market === "KR" ? "secondary" : "outline"}
                      className="mt-0.5 shrink-0"
                    >
                      {item.market}
                    </Badge>
                    <div className="flex-1 min-w-0">
                      <a
                        href={item.url}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="font-medium hover:text-primary flex items-start gap-1 group"
                      >
                        {item.title}
                        <ExternalLink className="h-3.5 w-3.5 mt-0.5 shrink-0 opacity-0 group-hover:opacity-50" />
                      </a>
                      {item.summary && (
                        <p className="text-sm text-muted-foreground mt-1 line-clamp-2">
                          {item.summary}
                        </p>
                      )}
                      <div className="flex items-center gap-2 mt-2 text-xs text-muted-foreground">
                        <span>{item.source}</span>
                        <span>·</span>
                        <span>{formatDate(item.publishedAt)}</span>
                        {item.relatedTicker && (
                          <>
                            <span>·</span>
                            <span className="text-primary">{item.relatedTicker}</span>
                          </>
                        )}
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>
            ))}
            {(!news || news.length === 0) && (
              <Card>
                <CardContent className="py-16 text-center text-muted-foreground">
                  뉴스가 없습니다
                </CardContent>
              </Card>
            )}
          </div>

          {/* Pagination */}
          {meta && meta.totalPages && meta.totalPages > 1 && (
            <div className="flex items-center justify-center gap-4">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="p-2 rounded hover:bg-accent disabled:opacity-30"
              >
                <ChevronLeft className="h-5 w-5" />
              </button>
              <span className="text-sm">
                {page + 1} / {meta.totalPages}
              </span>
              <button
                onClick={() => setPage((p) => p + 1)}
                disabled={page >= (meta.totalPages || 1) - 1}
                className="p-2 rounded hover:bg-accent disabled:opacity-30"
              >
                <ChevronRight className="h-5 w-5" />
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
}
