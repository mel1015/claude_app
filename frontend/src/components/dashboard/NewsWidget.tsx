"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import type { NewsDto } from "@/lib/types";
import { formatDate } from "@/lib/utils";
import { Newspaper, ExternalLink } from "lucide-react";
import { Badge } from "@/components/ui/Badge";

interface NewsWidgetProps {
  news: NewsDto[];
}

export function NewsWidget({ news }: NewsWidgetProps) {
  return (
    <Card>
      <CardHeader className="pb-3">
        <CardTitle className="flex items-center gap-2 text-base">
          <Newspaper className="h-4 w-4" />
          최신 뉴스
        </CardTitle>
      </CardHeader>
      <CardContent className="p-0">
        <div className="divide-y">
          {news?.map((item) => (
            <div key={item.id} className="px-4 py-3 hover:bg-muted/30">
              <div className="flex items-start justify-between gap-2">
                <a
                  href={item.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-sm font-medium hover:text-primary line-clamp-2 flex-1"
                >
                  {item.title}
                  <ExternalLink className="inline h-3 w-3 ml-1 opacity-50" />
                </a>
                <Badge variant={item.market === "KR" ? "secondary" : "outline"} className="shrink-0">
                  {item.market}
                </Badge>
              </div>
              <div className="flex items-center gap-2 mt-1">
                <span className="text-xs text-muted-foreground">{item.source}</span>
                <span className="text-xs text-muted-foreground">·</span>
                <span className="text-xs text-muted-foreground">{formatDate(item.publishedAt)}</span>
              </div>
            </div>
          ))}
          {(!news || news.length === 0) && (
            <div className="text-center py-8 text-muted-foreground text-sm">뉴스 없음</div>
          )}
        </div>
      </CardContent>
    </Card>
  );
}
