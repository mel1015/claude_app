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
import { Star, StarOff, Brain, TrendingUp, TrendingDown, Minus, Target, BarChart3, Globe, ShieldAlert, Loader2 } from "lucide-react";
import { api } from "@/lib/api";
import type { StockAnalysisReport } from "@/lib/types";

export default function StockDetailPage() {
  const params = useParams();
  const searchParams = useSearchParams();
  const ticker = params.ticker as string;
  const market = searchParams.get("market") || "KOSPI";

  const [days, setDays] = useState(90);
  const [analysis, setAnalysis] = useState<StockAnalysisReport | null>(null);
  const [analyzing, setAnalyzing] = useState(false);
  const [analysisError, setAnalysisError] = useState<string | null>(null);
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

  const handleAnalyze = async () => {
    setAnalyzing(true);
    setAnalysisError(null);
    try {
      const res = await api.post<{ data: StockAnalysisReport; meta: { error?: string } }>(
        `/api/v1/stocks/${market}/${ticker}/analyze`
      );
      if (res.meta?.error) {
        setAnalysisError(res.meta.error);
      } else if (res.data && Object.keys(res.data).length > 0) {
        setAnalysis(res.data);
      } else {
        setAnalysisError("AI 분석 결과를 가져올 수 없습니다.");
      }
    } catch {
      setAnalysisError("AI 분석 요청에 실패했습니다.");
    } finally {
      setAnalyzing(false);
    }
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

      {/* AI Analysis */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="flex items-center gap-2 text-base">
              <Brain className="h-5 w-5 text-purple-500" />
              AI 종합 분석
            </CardTitle>
            <button
              onClick={handleAnalyze}
              disabled={analyzing}
              className="flex items-center gap-2 px-4 py-2 bg-purple-600 text-white rounded-md text-sm font-medium hover:bg-purple-700 disabled:opacity-50"
            >
              {analyzing ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  분석 중...
                </>
              ) : (
                <>
                  <Brain className="h-4 w-4" />
                  {analysis ? "재분석" : "AI 분석 실행"}
                </>
              )}
            </button>
          </div>
        </CardHeader>
        <CardContent>
          {analysisError && (
            <div className="text-sm text-destructive text-center py-4">{analysisError}</div>
          )}
          {!analysis && !analysisError && !analyzing && (
            <div className="text-sm text-muted-foreground text-center py-8">
              AI 분석 실행 버튼을 눌러 Gemini AI의 종합 투자 분석을 확인하세요.
            </div>
          )}
          {analyzing && !analysis && (
            <div className="flex flex-col items-center gap-3 py-8">
              <Loader2 className="h-8 w-8 animate-spin text-purple-500" />
              <p className="text-sm text-muted-foreground">AI가 종목을 분석하고 있습니다...</p>
            </div>
          )}
          {analysis && (
            <div className="space-y-4">
              {/* Header: recommendation + score */}
              <div className="flex items-center justify-between flex-wrap gap-3 bg-muted/50 rounded-lg p-4">
                <div className="flex items-center gap-3">
                  <span className="text-sm text-muted-foreground">투자 의견</span>
                  {analysis.recommendation === "매수" && (
                    <Badge variant="success" className="gap-1 text-base px-3 py-1">
                      <TrendingUp className="h-4 w-4" />{analysis.recommendation}
                    </Badge>
                  )}
                  {analysis.recommendation === "매도" && (
                    <Badge variant="destructive" className="gap-1 text-base px-3 py-1">
                      <TrendingDown className="h-4 w-4" />{analysis.recommendation}
                    </Badge>
                  )}
                  {analysis.recommendation === "관망" && (
                    <Badge variant="secondary" className="gap-1 text-base px-3 py-1">
                      <Minus className="h-4 w-4" />{analysis.recommendation}
                    </Badge>
                  )}
                </div>
                <div className="flex items-center gap-4">
                  <div className="text-center">
                    <div className="text-xs text-muted-foreground">점수</div>
                    <div className={cn("text-2xl font-bold",
                      analysis.score >= 7 ? "text-green-500" : analysis.score >= 4 ? "text-yellow-500" : "text-red-500"
                    )}>
                      {analysis.score}/10
                    </div>
                  </div>
                  {analysis.targetPrice && (
                    <div className="text-center">
                      <div className="text-xs text-muted-foreground">목표가</div>
                      <div className="font-mono text-sm">
                        {analysis.targetPrice.low?.toLocaleString()}~{analysis.targetPrice.high?.toLocaleString()}
                      </div>
                    </div>
                  )}
                  {analysis.stopLoss != null && (
                    <div className="text-center">
                      <div className="text-xs text-muted-foreground">손절가</div>
                      <div className="font-mono text-sm text-red-500">
                        {analysis.stopLoss.toLocaleString()}
                      </div>
                    </div>
                  )}
                </div>
              </div>

              {/* Summary */}
              {analysis.summary && (
                <div className="bg-purple-500/10 rounded-lg p-4">
                  <p className="text-sm font-medium leading-relaxed">{analysis.summary}</p>
                </div>
              )}

              {/* Detail sections */}
              <div className="grid gap-3 md:grid-cols-2">
                {analysis.technicalAnalysis && (
                  <div className="bg-muted/50 rounded-lg p-4 space-y-2">
                    <div className="flex items-center gap-2 text-sm font-medium">
                      <BarChart3 className="h-4 w-4 text-blue-500" />
                      차트 분석
                    </div>
                    <p className="text-sm text-muted-foreground leading-relaxed">{analysis.technicalAnalysis}</p>
                  </div>
                )}
                {analysis.fundamentalNote && (
                  <div className="bg-muted/50 rounded-lg p-4 space-y-2">
                    <div className="flex items-center gap-2 text-sm font-medium">
                      <Target className="h-4 w-4 text-green-500" />
                      펀더멘털
                    </div>
                    <p className="text-sm text-muted-foreground leading-relaxed">{analysis.fundamentalNote}</p>
                  </div>
                )}
                {analysis.globalContext && (
                  <div className="bg-muted/50 rounded-lg p-4 space-y-2">
                    <div className="flex items-center gap-2 text-sm font-medium">
                      <Globe className="h-4 w-4 text-orange-500" />
                      글로벌/경제 정세
                    </div>
                    <p className="text-sm text-muted-foreground leading-relaxed">{analysis.globalContext}</p>
                  </div>
                )}
                {analysis.riskFactors && (
                  <div className="bg-muted/50 rounded-lg p-4 space-y-2">
                    <div className="flex items-center gap-2 text-sm font-medium">
                      <ShieldAlert className="h-4 w-4 text-red-500" />
                      리스크 요인
                    </div>
                    <p className="text-sm text-muted-foreground leading-relaxed">{analysis.riskFactors}</p>
                  </div>
                )}
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
