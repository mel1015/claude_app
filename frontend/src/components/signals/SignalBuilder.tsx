"use client";

import { useState, useEffect } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Plus, Trash2, Sparkles, Loader2 } from "lucide-react";
import { api } from "@/lib/api";
import type { SignalCondition, SignalLeaf, SignalGroup } from "@/lib/types";

const FIELDS = [
  { value: "close_price", label: "종가" },
  { value: "open_price", label: "시가" },
  { value: "high_price", label: "고가" },
  { value: "low_price", label: "저가" },
  { value: "volume", label: "거래량" },
  { value: "change_rate", label: "등락률(%)" },
  { value: "ma5", label: "MA5" },
  { value: "ma20", label: "MA20" },
  { value: "ma60", label: "MA60" },
  { value: "rsi14", label: "RSI(14)" },
  { value: "macd", label: "MACD" },
  { value: "macd_signal", label: "MACD Signal" },
  { value: "macd_hist", label: "MACD Hist" },
];

const OPERATORS = [
  { value: ">", label: ">" },
  { value: ">=", label: ">=" },
  { value: "<", label: "<" },
  { value: "<=", label: "<=" },
  { value: "==", label: "==" },
  { value: "!=", label: "!=" },
];

const schema = z.object({
  name: z.string().min(1, "이름을 입력하세요"),
  marketFilter: z.enum(["ALL", "KR", "US"]),
  active: z.boolean(),
});

type FormData = z.infer<typeof schema>;

let idCounter = 0;
const genId = () => `node_${++idCounter}`;

function createLeaf(): SignalLeaf {
  return { id: genId(), field: "close_price", operator: ">", value: 0 };
}

function createGroup(): SignalGroup {
  return { id: genId(), logic: "AND", conditions: [createLeaf()] };
}

function LeafValueInput({ value, onChange }: { value: number; onChange: (v: number) => void }) {
  const [display, setDisplay] = useState(String(value));

  useEffect(() => {
    setDisplay(String(value));
  }, [value]);

  return (
    <input
      type="text"
      inputMode="decimal"
      value={display}
      onChange={(e) => {
        const raw = e.target.value;
        // 선행 0 제거: "-020" → "-20", "020" → "20", "0.5"는 유지
        const normalized = raw.replace(/^(-?)0+(?=[1-9])/, "$1");
        setDisplay(normalized);
        const num = parseFloat(normalized);
        onChange(isNaN(num) ? 0 : num);
      }}
      onBlur={() => {
        const num = parseFloat(display) || 0;
        setDisplay(String(num));
        onChange(num);
      }}
      className="text-xs border rounded px-2 py-1 w-24 bg-background"
    />
  );
}

interface SignalBuilderProps {
  onSaved?: () => void;
  onCancel?: () => void;
  initialData?: {
    id: string;
    name: string;
    marketFilter: string;
    active: boolean;
    conditions: SignalCondition;
  };
}

export function SignalBuilder({ onSaved, onCancel, initialData }: SignalBuilderProps) {
  const [conditions, setConditions] = useState<SignalCondition>(
    initialData?.conditions || { version: "1.0", logic: "AND", conditions: [createLeaf()] }
  );
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [analyzing, setAnalyzing] = useState(false);
  const [analysisResult, setAnalysisResult] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    getValues,
    formState: { errors },
  } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: initialData?.name || "",
      marketFilter: (initialData?.marketFilter as "ALL" | "KR" | "US") || "ALL",
      active: initialData?.active ?? true,
    },
  });

  const onSubmit = async (data: FormData) => {
    setSaving(true);
    setError(null);
    try {
      const payload = { ...data, conditions: JSON.stringify(conditions) };
      if (initialData?.id) {
        await api.put(`/api/v1/signals/${initialData.id}`, payload);
      } else {
        await api.post("/api/v1/signals", payload);
      }
      onSaved?.();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : "저장 실패");
    } finally {
      setSaving(false);
    }
  };

  const onAnalyze = async () => {
    setAnalyzing(true);
    setAnalysisResult(null);
    try {
      const formValues = getValues();
      const payload = {
        name: formValues.name || "(이름 없음)",
        marketFilter: formValues.marketFilter || "ALL",
        conditions: JSON.stringify(conditions),
      };
      const res = await api.post<{ data: { analysis: string } }>("/api/v1/signals/analyze", payload);
      setAnalysisResult(res.data?.analysis ?? "분석 결과 없음");
    } catch (err: unknown) {
      setAnalysisResult(err instanceof Error ? err.message : "분석 실패");
    } finally {
      setAnalyzing(false);
    }
  };

  const updateLeaf = (path: number[], updates: Partial<SignalLeaf>) => {
    setConditions((prev) => {
      const updated = JSON.parse(JSON.stringify(prev)) as SignalCondition;
      let node: SignalCondition | SignalGroup = updated;
      for (const idx of path.slice(0, -1)) {
        node = (node.conditions[idx] as SignalGroup);
      }
      Object.assign(node.conditions[path[path.length - 1]], updates);
      return updated;
    });
  };

  const addNode = (path: number[], type: "leaf" | "group") => {
    setConditions((prev) => {
      const updated = JSON.parse(JSON.stringify(prev)) as SignalCondition;
      let node: SignalCondition | SignalGroup = updated;
      for (const idx of path) node = (node.conditions[idx] as SignalGroup);
      node.conditions.push(type === "leaf" ? createLeaf() : createGroup());
      return updated;
    });
  };

  const removeNode = (path: number[]) => {
    setConditions((prev) => {
      const updated = JSON.parse(JSON.stringify(prev)) as SignalCondition;
      let parent: SignalCondition | SignalGroup = updated;
      for (const idx of path.slice(0, -1)) parent = (parent.conditions[idx] as SignalGroup);
      parent.conditions.splice(path[path.length - 1], 1);
      return updated;
    });
  };

  const renderNode = (node: SignalLeaf | SignalGroup, path: number[]) => {
    if ("conditions" in node) {
      const group = node as SignalGroup;
      return (
        <div key={group.id} className="border border-dashed rounded-lg p-3 space-y-2 bg-muted/20">
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => {
                setConditions((prev) => {
                  const updated = JSON.parse(JSON.stringify(prev)) as SignalCondition;
                  if (path.length === 0) {
                    updated.logic = updated.logic === "AND" ? "OR" : "AND";
                  } else {
                    let n: SignalCondition | SignalGroup = updated;
                    for (const idx of path.slice(0, -1)) n = (n.conditions[idx] as SignalGroup);
                    const g = n.conditions[path[path.length - 1]] as SignalGroup;
                    g.logic = g.logic === "AND" ? "OR" : "AND";
                  }
                  return updated;
                });
              }}
              className={`px-3 py-1 text-xs font-bold rounded ${
                group.logic === "AND"
                  ? "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400"
                  : "bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400"
              }`}
            >
              {group.logic}
            </button>
            <span className="text-xs text-muted-foreground">그룹</span>
            {path.length > 0 && (
              <button
                type="button"
                onClick={() => removeNode(path)}
                className="ml-auto p-1 text-destructive hover:bg-destructive/10 rounded"
              >
                <Trash2 className="h-3 w-3" />
              </button>
            )}
          </div>
          <div className="space-y-2 pl-4">
            {group.conditions.map((child, idx) => renderNode(child, [...path, idx]))}
          </div>
          <div className="flex gap-2 pl-4">
            <button
              type="button"
              onClick={() => addNode(path, "leaf")}
              className="text-xs px-2 py-1 rounded border hover:bg-accent flex items-center gap-1"
            >
              <Plus className="h-3 w-3" /> 조건 추가
            </button>
            <button
              type="button"
              onClick={() => addNode(path, "group")}
              className="text-xs px-2 py-1 rounded border hover:bg-accent flex items-center gap-1"
            >
              <Plus className="h-3 w-3" /> 그룹 추가
            </button>
          </div>
        </div>
      );
    } else {
      const leaf = node as SignalLeaf;
      return (
        <div key={leaf.id} className="flex items-center gap-2 flex-wrap">
          <select
            value={leaf.field}
            onChange={(e) => updateLeaf(path, { field: e.target.value })}
            className="text-xs border rounded px-2 py-1 bg-background"
          >
            {FIELDS.map((f) => (
              <option key={f.value} value={f.value}>{f.label}</option>
            ))}
          </select>
          <select
            value={leaf.operator}
            onChange={(e) => updateLeaf(path, { operator: e.target.value as SignalLeaf["operator"] })}
            className="text-xs border rounded px-2 py-1 bg-background"
          >
            {OPERATORS.map((op) => (
              <option key={op.value} value={op.value}>{op.label}</option>
            ))}
          </select>
          <LeafValueInput
            value={leaf.value}
            onChange={(v) => updateLeaf(path, { value: v })}
          />
          <button
            type="button"
            onClick={() => removeNode(path)}
            className="p-1 text-destructive hover:bg-destructive/10 rounded"
          >
            <Trash2 className="h-3 w-3" />
          </button>
        </div>
      );
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div>
          <label className="text-sm font-medium">시그널 이름</label>
          <input
            {...register("name")}
            className="mt-1 w-full border rounded-md px-3 py-2 text-sm bg-background"
            placeholder="예: RSI 과매도 + 고거래량"
          />
          {errors.name && <p className="text-xs text-destructive mt-1">{errors.name.message}</p>}
        </div>
        <div>
          <label className="text-sm font-medium">마켓 필터</label>
          <select
            {...register("marketFilter")}
            className="mt-1 w-full border rounded-md px-3 py-2 text-sm bg-background"
          >
            <option value="ALL">전체</option>
            <option value="KR">한국 (KOSPI/KOSDAQ)</option>
            <option value="US">미국 (NYSE/NASDAQ)</option>
          </select>
        </div>
        <div className="flex items-end">
          <label className="flex items-center gap-2 cursor-pointer">
            <input type="checkbox" {...register("active")} className="rounded" />
            <span className="text-sm font-medium">활성화</span>
          </label>
        </div>
      </div>

      <div>
        <label className="text-sm font-medium mb-2 block">조건 설정</label>
        <div className="border rounded-lg p-4 space-y-2 bg-muted/10">
          <div className="flex items-center gap-2 mb-3">
            <button
              type="button"
              onClick={() =>
                setConditions((prev) => ({ ...prev, logic: prev.logic === "AND" ? "OR" : "AND" }))
              }
              className={`px-3 py-1 text-xs font-bold rounded ${
                conditions.logic === "AND"
                  ? "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400"
                  : "bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400"
              }`}
            >
              {conditions.logic}
            </button>
            <span className="text-xs text-muted-foreground">루트 그룹</span>
          </div>
          <div className="space-y-2 pl-4">
            {conditions.conditions.map((node, idx) => renderNode(node, [idx]))}
          </div>
          <div className="flex gap-2 pl-4 pt-2">
            <button
              type="button"
              onClick={() =>
                setConditions((prev) => ({ ...prev, conditions: [...prev.conditions, createLeaf()] }))
              }
              className="text-xs px-2 py-1 rounded border hover:bg-accent flex items-center gap-1"
            >
              <Plus className="h-3 w-3" /> 조건 추가
            </button>
            <button
              type="button"
              onClick={() =>
                setConditions((prev) => ({ ...prev, conditions: [...prev.conditions, createGroup()] }))
              }
              className="text-xs px-2 py-1 rounded border hover:bg-accent flex items-center gap-1"
            >
              <Plus className="h-3 w-3" /> 그룹 추가
            </button>
          </div>
        </div>
      </div>

      <div>
        <button
          type="button"
          onClick={onAnalyze}
          disabled={analyzing}
          className="flex items-center gap-2 px-4 py-2 text-sm border rounded-md hover:bg-accent disabled:opacity-50"
        >
          {analyzing ? (
            <Loader2 className="h-4 w-4 animate-spin" />
          ) : (
            <Sparkles className="h-4 w-4 text-yellow-500" />
          )}
          {analyzing ? "Gemini 분석 중..." : "Gemini AI 전략 분석"}
        </button>

        {analysisResult && (
          <div className="mt-3 p-4 rounded-lg border bg-muted/20 text-sm whitespace-pre-wrap leading-relaxed">
            <p className="font-semibold text-xs text-muted-foreground mb-2 flex items-center gap-1">
              <Sparkles className="h-3 w-3 text-yellow-500" /> Gemini 전략 적합도 분석
            </p>
            {analysisResult}
          </div>
        )}
      </div>

      {error && <p className="text-sm text-destructive">{error}</p>}

      <div className="flex items-center gap-3">
        <button
          type="submit"
          disabled={saving}
          className="px-6 py-2 bg-primary text-primary-foreground rounded-md text-sm font-medium hover:bg-primary/90 disabled:opacity-50"
        >
          {saving ? "저장 중..." : initialData ? "수정하기" : "시그널 생성"}
        </button>
        {onCancel && (
          <button
            type="button"
            onClick={onCancel}
            className="px-4 py-2 border rounded-md text-sm font-medium hover:bg-accent"
          >
            취소
          </button>
        )}
      </div>
    </form>
  );
}
