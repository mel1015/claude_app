import useSWR from "swr";
import { fetchApi } from "@/lib/api";
import type { ApiResponse, StockDto, DashboardDto, FavoriteDto, NewsDto, SignalDto } from "@/lib/types";

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const fetcher = (url: string) => fetchApi<any>(url);

export function useDashboard() {
  const { data, error, isLoading, mutate } = useSWR<ApiResponse<DashboardDto>>(
    "/api/v1/dashboard",
    fetcher,
    { refreshInterval: 5 * 60 * 1000 }
  );
  return { dashboard: data?.data, meta: data?.meta, error, isLoading, mutate };
}

export function useStocks(market = "ALL", query?: string, page = 0, size = 20) {
  const params = new URLSearchParams({ market, page: String(page), size: String(size) });
  if (query) params.append("query", query);

  const { data, error, isLoading, mutate } = useSWR<ApiResponse<StockDto[]>>(
    `/api/v1/stocks?${params}`,
    fetcher
  );
  return { stocks: data?.data, meta: data?.meta, error, isLoading, mutate };
}

export function useStock(market: string, ticker: string) {
  const { data, error, isLoading } = useSWR<ApiResponse<StockDto>>(
    market && ticker ? `/api/v1/stocks/${market}/${ticker}` : null,
    fetcher
  );
  return { stock: data?.data, error, isLoading };
}

export function useStockHistory(market: string, ticker: string, days = 90) {
  const { data, error, isLoading } = useSWR<ApiResponse<StockDto[]>>(
    market && ticker ? `/api/v1/stocks/${market}/${ticker}/history?days=${days}` : null,
    fetcher
  );
  return { history: data?.data, error, isLoading };
}

export function useTopVolume(market = "ALL") {
  const { data, error, isLoading, mutate } = useSWR<ApiResponse<StockDto[]>>(
    `/api/v1/stocks/top-volume?market=${market}`,
    fetcher,
    { refreshInterval: 10 * 60 * 1000 }
  );
  return { stocks: data?.data, error, isLoading, mutate };
}

export function useFavorites() {
  const { data, error, isLoading, mutate } = useSWR<ApiResponse<FavoriteDto[]>>(
    "/api/v1/favorites",
    fetcher
  );
  return { favorites: data?.data, error, isLoading, mutate };
}

export function useNews(market = "ALL", ticker?: string, page = 0, size = 20) {
  const params = new URLSearchParams({ market, page: String(page), size: String(size) });
  if (ticker) params.append("ticker", ticker);

  const { data, error, isLoading, mutate } = useSWR<ApiResponse<NewsDto[]>>(
    `/api/v1/news?${params}`,
    fetcher,
    { refreshInterval: 15 * 60 * 1000 }
  );
  return { news: data?.data, meta: data?.meta, error, isLoading, mutate };
}

export function useSignals() {
  const { data, error, isLoading, mutate } = useSWR<ApiResponse<SignalDto[]>>(
    "/api/v1/signals",
    fetcher
  );
  return { signals: data?.data, error, isLoading, mutate };
}

export function useSignal(id?: string) {
  const { data, error, isLoading, mutate } = useSWR<ApiResponse<SignalDto>>(
    id ? `/api/v1/signals/${id}` : null,
    fetcher
  );
  return { signal: data?.data, error, isLoading, mutate };
}
