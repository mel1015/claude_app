# Frontend — Next.js 14 / TypeScript

## Layout

| Directory | Role |
|-----------|------|
| `src/app/` | App Router pages — `/`, `/stocks`, `/stocks/[ticker]`, `/favorites`, `/top-volume`, `/signals`, `/signals/builder`, `/news` |
| `src/components/` | Shared UI (Radix primitives wrapped for app use, Tailwind styles) |
| `src/hooks/` | SWR data hooks (`useStocks.ts`, etc.) |
| `src/lib/` | `api.ts` (fetch client), `types.ts` (shared DTOs), utils |
| `src/__tests__/` | Vitest + Testing Library tests |

## Key files

| File | Role |
|------|------|
| `src/lib/api.ts` | `fetchApi<T>(url)` — generic type parameter is required |
| `src/lib/types.ts` | Shared response/request types (StockDto, SignalDto, NewsDto, etc.) |
| `src/hooks/useStocks.ts` | Reference SWR hook pattern |

## Stack

- Data: SWR
- UI: Tailwind CSS + Radix UI (`@radix-ui/react-*`)
- Forms: `react-hook-form` + `zod`
- Charts: `recharts` (candlestick via `PriceChart.tsx`)
- Theme: `next-themes`

## Commands

```bash
npm install
npm run dev          # :3000
npm run build
npm run test         # vitest run (single)
npm run test:watch
npm run test:ui
npm run lint
```

## Backend dependency

Dev mode expects backend at `http://localhost:8080`. API calls fail until it is running and has collected data at least once.

## Rules

See `../.claude/rules/frontend-nextjs.md` and `testing.md` for enforced conventions.
