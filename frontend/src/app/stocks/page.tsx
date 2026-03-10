"use client";

import { useState, useCallback } from "react";
import { Search } from "lucide-react";
import { useStocks } from "@/hooks/useStocks";
import { StockTable } from "@/components/stocks/StockTable";
import { LoadingSpinner } from "@/components/ui/LoadingSpinner";
import { ErrorMessage } from "@/components/ui/ErrorMessage";
import { cn } from "@/lib/utils";

const TABS = [
  { value: "ALL", label: "전체" },
  { value: "KOSPI", label: "KOSPI" },
  { value: "KOSDAQ", label: "KOSDAQ" },
  { value: "NYSE", label: "NYSE" },
  { value: "NASDAQ", label: "NASDAQ" },
];

const PAGE_SIZE = 20;

type PaginationButton = {
  label: string;
  onClick: () => void;
  disabled: boolean;
};

export default function StocksPage() {
  const [market, setMarket] = useState("ALL");
  const [query, setQuery] = useState("");
  const [inputValue, setInputValue] = useState("");
  const [page, setPage] = useState(0);

  const { stocks, meta, isLoading, error, mutate } = useStocks(market, query || undefined, page, PAGE_SIZE);

  const handleSearch = useCallback((e: React.FormEvent) => {
    e.preventDefault();
    setQuery(inputValue.trim());
    setPage(0);
  }, [inputValue]);

  const handleTabChange = useCallback((value: string) => {
    setMarket(value);
    setPage(0);
  }, []);

  const handleClear = useCallback(() => {
    setInputValue("");
    setQuery("");
    setPage(0);
  }, []);

  const totalPages = meta?.totalPages ?? 0;
  const totalElements = meta?.totalElements ?? 0;

  // 첫 로딩(데이터 없음)과 재조회(이전 데이터 유지) 구분
  const isInitialLoading = isLoading && stocks === undefined;

  const paginationButtons: PaginationButton[] = [
    { label: "처음", onClick: () => setPage(0), disabled: page === 0 },
    { label: "이전", onClick: () => setPage((p) => Math.max(0, p - 1)), disabled: page === 0 },
    { label: "다음", onClick: () => setPage((p) => Math.min(totalPages - 1, p + 1)), disabled: page >= totalPages - 1 },
    { label: "마지막", onClick: () => setPage(totalPages - 1), disabled: page >= totalPages - 1 },
  ];

  return (
    <div className="container mx-auto px-4 py-6 space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">주식 검색</h1>
        {!isInitialLoading && (
          <span className="text-sm text-muted-foreground">
            총 {totalElements.toLocaleString()}개 종목
          </span>
        )}
      </div>

      {/* 검색창 */}
      <form onSubmit={handleSearch} className="flex gap-2">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <input
            type="text"
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            placeholder="종목코드 또는 종목명 검색"
            className="w-full pl-9 pr-4 py-2 border rounded-md text-sm bg-background focus:outline-none focus:ring-2 focus:ring-primary"
          />
        </div>
        <button
          type="submit"
          className="px-4 py-2 bg-primary text-primary-foreground rounded-md text-sm font-medium hover:bg-primary/90"
        >
          검색
        </button>
        {(query || inputValue) && (
          <button
            type="button"
            onClick={handleClear}
            className="px-4 py-2 border rounded-md text-sm font-medium hover:bg-accent"
          >
            초기화
          </button>
        )}
      </form>

      {/* 마켓 탭 */}
      <div className="flex gap-1 border-b">
        {TABS.map((tab) => (
          <button
            key={tab.value}
            onClick={() => handleTabChange(tab.value)}
            className={cn(
              "px-4 py-2 text-sm font-medium border-b-2 transition-colors",
              market === tab.value
                ? "border-primary text-primary"
                : "border-transparent text-muted-foreground hover:text-foreground"
            )}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* 결과 */}
      {isInitialLoading ? (
        <div className="flex justify-center py-20">
          <LoadingSpinner />
        </div>
      ) : error ? (
        <ErrorMessage message="데이터를 불러오지 못했습니다." />
      ) : (
        <div className={cn("space-y-4", isLoading && "opacity-50 pointer-events-none")}>
          <div className="border rounded-lg overflow-hidden">
            <StockTable stocks={stocks ?? []} onFavoriteToggle={mutate} />
          </div>

          {/* 페이지네이션 */}
          {totalPages > 1 && (
            <div className="flex items-center justify-center gap-2">
              {paginationButtons.slice(0, 2).map((btn) => (
                <button
                  key={btn.label}
                  onClick={btn.onClick}
                  disabled={btn.disabled}
                  className="px-3 py-1 text-sm border rounded hover:bg-accent disabled:opacity-40"
                >
                  {btn.label}
                </button>
              ))}
              <span className="text-sm text-muted-foreground px-2">
                {page + 1} / {totalPages}
              </span>
              {paginationButtons.slice(2).map((btn) => (
                <button
                  key={btn.label}
                  onClick={btn.onClick}
                  disabled={btn.disabled}
                  className="px-3 py-1 text-sm border rounded hover:bg-accent disabled:opacity-40"
                >
                  {btn.label}
                </button>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
