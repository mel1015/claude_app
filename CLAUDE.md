# CLAUDE.md — Project Guide

## Overview

Daily stock report for Korea (KOSPI / KOSDAQ) and US (NYSE / NASDAQ) markets.

- **Backend**: Spring Boot 4.0.3 / Java 25 — `backend/`
- **Frontend**: Next.js 14 / TypeScript — `frontend/`
- **Database**: MongoDB 7 via Docker — collections: `stock_daily_cache`, `news_cache`, `favorites`, `signals`

## Stack & prerequisites

- Java Temurin 25
- Node.js 18+
- Docker (for MongoDB)
- `backend/.env` with `GEMINI_API_KEY` (and optionally `SLACK_WEBHOOK_URL`) — gitignored

## Run commands

```bash
# MongoDB
docker start mongo-stockreport
# first time: docker run -d --name mongo-stockreport -p 27017:27017 mongo:7

# Backend (:8080)
cd backend && ./gradlew bootRun

# Frontend (:3000)
cd frontend && npm run dev
```

Spring Boot 4 renamed the Mongo URI key: use `spring.mongodb.uri` (not `spring.data.mongodb.uri`). Profiles: `application.yml` → `stockreport` (prod), `application-dev.yml` → `stockreport-dev` (default active).

## Repository map

```
claude_app/
├── backend/           Spring Boot 4 — see backend/CLAUDE.md
├── frontend/          Next.js 14 — see frontend/CLAUDE.md
├── docs/              Standalone documents (Claude Code tool guide, etc.)
├── .claude/rules/     Scoped rules auto-loaded per path
├── CONTRIBUTING.md    Branch & commit conventions
└── README.md          User-facing setup + full API endpoint list
```

## Cross-cutting architecture

### Data collection pipeline

- `StartupDataCollector` runs KR and US collection in parallel via `CompletableFuture.allOf()` on boot.
- Market-level skip: if DB latest `tradeDate` equals today, the entire market is skipped (one query).
- Per-ticker skip: if today's row exists, no external API call is made.
- Incremental fetch: KR pulls last 5 rows, US pulls `5d` range. First-time collection pulls ~90 days (KR) / 3 months (US).
- Every external call is wrapped with Resilience4j (`RateLimiter + CircuitBreaker + Retry`). Do not add manual sleeps or retries on top.

### Signal automation

- `KrDataScheduler` / `UsDataScheduler` publish `DataCollectionCompletedEvent` when a batch finishes.
- `SignalScheduler` consumes it via `@EventListener` and runs all active signals `@Async`.
- Matches trigger `SlackNotificationService` (no-op when `SLACK_WEBHOOK_URL` is unset).

### MongoDB domain

- `StockDailyCache` unique key = `ticker + market + tradeDate + timeframe`. Always pass `Timeframe.DAILY` (or the relevant one) — missing it mixes weekly/monthly rows.
- `tradeDate` is stored as KST-midnight → UTC. Direct `mongosh` queries must use ISODate range, not string comparison. Details in `.claude/rules/mongodb-queries.md`.
- `StockService.getLatestStock` uses `findFirst…OrderByTradeDateDesc` to avoid per-ticker date drift.

## Capability boundaries

- No production deployment pipeline — local development only.
- No real-time price streaming — batch collection via startup + schedulers only.
- No production DB access — development DB (`stockreport-dev`) only.
- External APIs (Naver, Yahoo, Gemini) are rate-limited — rely on the Resilience4j config; do not introduce manual retry loops.
- Secrets live in `backend/.env` (gitignored). Never commit API keys or hardcode them in yml.
- When a request conflicts with the current structure, state the reason and offer alternatives instead of silently substituting.

## References

- API endpoints (full list) → [README.md](./README.md#api-엔드포인트)
- Branch strategy + commit conventions → [CONTRIBUTING.md](./CONTRIBUTING.md)
- Claude Code tool guide (not project code) → [docs/CLAUDE_CODE_GUIDE.md](./docs/CLAUDE_CODE_GUIDE.md)
- Backend details → [backend/CLAUDE.md](./backend/CLAUDE.md)
- Frontend details → [frontend/CLAUDE.md](./frontend/CLAUDE.md)
- Scoped rules → [.claude/rules/](./.claude/rules/)
