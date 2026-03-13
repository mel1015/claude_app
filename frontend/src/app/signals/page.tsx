"use client";

import { useState } from "react";
import Link from "next/link";
import { useSignals } from "@/hooks/useStocks";
import { LoadingSpinner } from "@/components/ui/LoadingSpinner";
import { ErrorMessage } from "@/components/ui/ErrorMessage";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Bell, Plus, Play, Trash2, ChevronDown, ChevronUp, Pencil, X } from "lucide-react";
import { api } from "@/lib/api";
import { formatDate } from "@/lib/utils";
import type { SignalDto, StockDto } from "@/lib/types";
import { StockTable } from "@/components/stocks/StockTable";
import { SignalBuilder } from "@/components/signals/SignalBuilder";

export default function SignalsPage() {
  const { signals, error, isLoading, mutate } = useSignals();
  const [runningId, setRunningId] = useState<string | null>(null);
  const [results, setResults] = useState<Record<string, StockDto[]>>({});
  const [expanded, setExpanded] = useState<string | null>(null);
  const [editingSignal, setEditingSignal] = useState<SignalDto | null>(null);

  const handleRun = async (id: string) => {
    setRunningId(id);
    try {
      const res = await api.post<{ data: StockDto[] }>(`/api/v1/signals/${id}/run`);
      setResults((prev) => ({ ...prev, [id]: res.data }));
      setExpanded(id);
      mutate();
    } catch (err) {
      console.error("Run failed:", err);
    } finally {
      setRunningId(null);
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm("시그널을 삭제하시겠습니까?")) return;
    try {
      await api.delete(`/api/v1/signals/${id}`);
      mutate();
    } catch (err) {
      console.error("Delete failed:", err);
    }
  };

  const handleToggle = async (id: string, active: boolean) => {
    try {
      await api.put(`/api/v1/signals/${id}`, { active: !active });
      mutate();
    } catch (err) {
      console.error("Toggle failed:", err);
    }
  };

  if (isLoading) return <LoadingSpinner />;
  if (error) return <ErrorMessage message="시그널을 불러올 수 없습니다" />;

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="flex items-center gap-2 text-2xl font-bold">
          <Bell className="h-6 w-6" />
          커스텀 시그널
        </h1>
        <Link
          href="/signals/builder"
          className="flex items-center gap-2 px-4 py-2 bg-primary text-primary-foreground rounded-md text-sm font-medium hover:bg-primary/90"
        >
          <Plus className="h-4 w-4" />
          시그널 만들기
        </Link>
      </div>

      {!signals || signals.length === 0 ? (
        <Card>
          <CardContent className="py-16 text-center">
            <Bell className="h-12 w-12 mx-auto text-muted-foreground mb-4" />
            <p className="text-muted-foreground">등록된 시그널이 없습니다</p>
            <Link
              href="/signals/builder"
              className="text-primary hover:underline text-sm mt-2 inline-block"
            >
              첫 번째 시그널을 만들어 보세요 →
            </Link>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          {signals.map((signal) => (
            <Card key={signal.id}>
              <CardHeader className="pb-3">
                <div className="flex items-center justify-between flex-wrap gap-2">
                  <div className="flex items-center gap-3">
                    <CardTitle className="text-base">{signal.name}</CardTitle>
                    <Badge variant={signal.active ? "success" : "secondary"}>
                      {signal.active ? "활성" : "비활성"}
                    </Badge>
                    <Badge variant="outline">{signal.marketFilter}</Badge>
                  </div>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => handleToggle(signal.id, signal.active)}
                      className="px-3 py-1 text-xs rounded border hover:bg-accent"
                    >
                      {signal.active ? "비활성화" : "활성화"}
                    </button>
                    <button
                      onClick={() => handleRun(signal.id)}
                      disabled={runningId === signal.id}
                      className="flex items-center gap-1 px-3 py-1 text-xs bg-green-600 text-white rounded hover:bg-green-700 disabled:opacity-50"
                    >
                      <Play className="h-3 w-3" />
                      {runningId === signal.id ? "실행 중..." : "실행"}
                    </button>
                    <button
                      onClick={() => setEditingSignal(signal)}
                      className="p-1 rounded hover:bg-accent text-muted-foreground hover:text-foreground"
                      title="수정"
                    >
                      <Pencil className="h-4 w-4" />
                    </button>
                    <button
                      onClick={() => setExpanded(expanded === signal.id ? null : signal.id)}
                      className="p-1 rounded hover:bg-accent"
                    >
                      {expanded === signal.id ? (
                        <ChevronUp className="h-4 w-4" />
                      ) : (
                        <ChevronDown className="h-4 w-4" />
                      )}
                    </button>
                    <button
                      onClick={() => handleDelete(signal.id)}
                      className="p-1 hover:bg-destructive/10 rounded text-destructive"
                    >
                      <Trash2 className="h-4 w-4" />
                    </button>
                  </div>
                </div>
                {signal.lastRunAt && (
                  <div className="text-xs text-muted-foreground">
                    마지막 실행: {formatDate(signal.lastRunAt)}
                    {signal.lastResults && ` · ${signal.lastResults.length}개 매칭`}
                  </div>
                )}
              </CardHeader>

              {expanded === signal.id && (
                <CardContent>
                  <div className="space-y-4">
                    <div>
                      <div className="text-xs text-muted-foreground mb-2">조건 (JSON)</div>
                      <pre className="text-xs bg-muted p-3 rounded overflow-auto max-h-40">
                        {JSON.stringify(signal.conditions, null, 2)}
                      </pre>
                    </div>
                    {results[signal.id] !== undefined && (
                      <div>
                        <div className="text-xs text-muted-foreground mb-2">
                          실행 결과: {results[signal.id].length}개 종목 매칭
                        </div>
                        {results[signal.id].length > 0 ? (
                          <StockTable stocks={results[signal.id]} showFavoriteButton={false} />
                        ) : (
                          <div className="text-sm text-muted-foreground text-center py-4">
                            조건에 맞는 종목이 없습니다
                          </div>
                        )}
                      </div>
                    )}
                  </div>
                </CardContent>
              )}
            </Card>
          ))}
        </div>
      )}

      {/* 수정 모달 */}
      {editingSignal && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
          onClick={(e) => e.target === e.currentTarget && setEditingSignal(null)}
        >
          <div className="w-full max-w-3xl max-h-[90vh] overflow-y-auto bg-card rounded-xl shadow-xl">
            <div className="flex items-center justify-between p-6 border-b">
              <h2 className="flex items-center gap-2 text-lg font-bold">
                <Pencil className="h-5 w-5" />
                시그널 수정
              </h2>
              <button
                onClick={() => setEditingSignal(null)}
                className="p-2 rounded-md hover:bg-accent"
              >
                <X className="h-5 w-5" />
              </button>
            </div>
            <div className="p-6">
              <SignalBuilder
                initialData={{
                  id: editingSignal.id,
                  name: editingSignal.name,
                  marketFilter: editingSignal.marketFilter,
                  timeframe: editingSignal.timeframe,
                  active: editingSignal.active,
                  conditions: editingSignal.conditions,
                }}
                onSaved={() => {
                  setEditingSignal(null);
                  mutate();
                }}
                onCancel={() => setEditingSignal(null)}
              />
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
