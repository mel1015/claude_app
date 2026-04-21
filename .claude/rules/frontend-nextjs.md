---
name: Frontend Next.js Rules
description: Next.js 14 App Router, SWR, Tailwind, and API client conventions
paths:
  - frontend/**
---

# Frontend (Next.js 14 / TypeScript)

## API client

- Use `fetchApi<T>(url)` from `src/lib/api.ts`. The generic type parameter is REQUIRED — calling without it produces TypeScript errors and is the single most common frontend bug pattern here.
- Shared response/request types live in `src/lib/types.ts`. Reuse them; do not redefine per-component.

## Data fetching

- Use SWR. Place hooks under `src/hooks/` (e.g. `useStocks.ts`) — do not call `fetchApi` directly from page components.
- For polling (e.g. collection status banner), use SWR's `refreshInterval` option.

## Styling

- Tailwind CSS is the default for layout/typography.
- Radix UI primitives are allowed for dialogs, dropdowns, tabs, tooltips (see `package.json`).
- Theme toggle goes through `next-themes`.

## Forms & validation

- `react-hook-form` + `zod` via `@hookform/resolvers` for form schemas.

## Routing (App Router)

- Pages live under `src/app/`. Colocate route-specific components inside the route folder; promote only genuinely shared components to `src/components/`.

## Charts

- `recharts` is the chosen chart library. Candlestick views use `PriceChart.tsx`.
