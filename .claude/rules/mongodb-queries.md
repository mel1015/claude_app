---
name: MongoDB Query Rules
description: StockDailyCache keying, tradeDate ISODate handling, ALL-market query patterns
paths:
  - backend/**/domain/**
  - backend/**/service/**
---

# MongoDB Query Conventions

## `StockDailyCache` unique key

`ticker + market + tradeDate + timeframe`. Any finder missing `timeframe` will mix DAILY / WEEKLY / MONTHLY rows into the same result set.

- ALWAYS pass `Timeframe.DAILY` (or the relevant one) explicitly.
- Prefer `findFirstByTickerAndMarketAndTimeframeOrderByTradeDateDesc` over date-equality queries. Per-ticker latest collection dates can diverge (delistings, partial failures), so "latest today" is NOT a safe assumption.

## `tradeDate` ISODate quirk

`LocalDate` is persisted as KST midnight converted to UTC. So:

```
LocalDate 2026-03-12 (KST)  →  ISODate('2026-03-11T15:00:00.000Z') (UTC)
```

Java round-trips correctly — this quirk only affects direct `mongosh` queries. String comparison on `tradeDate` is WRONG. Use ISODate range:

```js
// rows for KST 2026-03-12
{ tradeDate: { $gte: ISODate('2026-03-11T15:00:00.000Z'),
               $lt:  ISODate('2026-03-12T15:00:00.000Z') } }
```

Past incident: data was wrongly reported as missing because the query used string comparison.

## ALL-market queries

`findByTimeframe` without a date filter returns ~90 days of rows → duplicate-looking results. Use instead:

- Stock list: `findLatestAllByTimeframe(krDate, usDate, timeframe, pageable)`
- Stock search: `searchByTickerOrNameLatest(query, krDate, usDate, timeframe, pageable)`

Resolve KR and US latest dates separately (`getLatestDate(Market.KOSPI)` / `getLatestDate(Market.NYSE)`) and combine with `$or` per market — a single `latestDate` will drop one side.
