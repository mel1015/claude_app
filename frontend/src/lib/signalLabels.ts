import type { SignalLeaf, SignalGroup } from "@/lib/types";

export const FIELDS = [
  { value: "close_price", label: "종가" },
  { value: "open_price", label: "시가" },
  { value: "high_price", label: "고가" },
  { value: "low_price", label: "저가" },
  { value: "volume", label: "거래량" },
  { value: "change_rate", label: "등락률(%)" },
  { value: "ma5", label: "MA5" },
  { value: "ma10", label: "MA10" },
  { value: "ma20", label: "MA20" },
  { value: "ma60", label: "MA60" },
  { value: "rsi14", label: "RSI(14)" },
  { value: "macd", label: "MACD" },
  { value: "macd_signal", label: "MACD Signal" },
  { value: "macd_hist", label: "MACD Hist" },
];

export const OPERATORS = [
  { value: ">", label: ">" },
  { value: ">=", label: ">=" },
  { value: "<", label: "<" },
  { value: "<=", label: "<=" },
  { value: "==", label: "==" },
  { value: "!=", label: "!=" },
  { value: "crossover", label: "상향돌파 ↑" },
  { value: "crossunder", label: "하향이탈 ↓" },
];

export const FIELD_LABELS: Record<string, string> = Object.fromEntries(
  FIELDS.map((f) => [f.value, f.label])
);

export const OP_LABELS: Record<string, string> = Object.fromEntries(
  OPERATORS.map((o) => [o.value, o.label])
);

export function isLeaf(node: SignalLeaf | SignalGroup): node is SignalLeaf {
  return "field" in node;
}
