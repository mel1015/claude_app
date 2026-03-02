import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export function formatNumber(num: number | null | undefined, decimals = 2): string {
  if (num === null || num === undefined) return "-";
  return num.toLocaleString("ko-KR", {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  });
}

export function formatPrice(price: number | null | undefined, market?: string): string {
  if (price === null || price === undefined) return "-";
  const isKr = market === "KOSPI" || market === "KOSDAQ";
  if (isKr) {
    return price.toLocaleString("ko-KR") + "원";
  }
  return "$" + price.toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

export function formatChangeRate(rate: number | null | undefined): string {
  if (rate === null || rate === undefined) return "-";
  const sign = rate > 0 ? "+" : "";
  return `${sign}${rate.toFixed(2)}%`;
}

export function getChangeColor(rate: number | null | undefined): string {
  if (rate === null || rate === undefined) return "text-gray-500";
  if (rate > 0) return "text-red-500";
  if (rate < 0) return "text-blue-500";
  return "text-gray-500";
}

export function formatVolume(vol: number | null | undefined): string {
  if (vol === null || vol === undefined) return "-";
  if (vol >= 1_000_000_000) return (vol / 1_000_000_000).toFixed(1) + "B";
  if (vol >= 1_000_000) return (vol / 1_000_000).toFixed(1) + "M";
  if (vol >= 1_000) return (vol / 1_000).toFixed(1) + "K";
  return vol.toString();
}

export function formatDate(dateStr: string | null | undefined): string {
  if (!dateStr) return "-";
  return new Date(dateStr).toLocaleDateString("ko-KR");
}
