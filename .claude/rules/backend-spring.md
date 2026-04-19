---
name: Backend Spring Boot 4 Rules
description: Spring Boot 4 / Java 25 conventions, Resilience4j usage, package layout
paths:
  - backend/**
---

# Backend (Spring Boot 4 / Java 25)

## Framework specifics

- Spring Boot 4 renamed the Mongo URI key: use `spring.mongodb.uri` (NOT the old `spring.data.mongodb.uri`). Both `application.yml` and `application-dev.yml` already use the new key.
- Active profile `dev` targets DB `stockreport-dev`; default/prod targets `stockreport`.
- Virtual Threads enabled via `spring.threads.virtual.enabled: true`. Blocking I/O inside request threads is safe — prefer it over reactive-style code unless there is a clear reason.

## Code conventions

- DTOs: use Java `record` for immutable response/request types.
- External API calls (Naver Finance, Yahoo Finance, Gemini) MUST be wrapped with Resilience4j (`RateLimiter + CircuitBreaker + Retry`). Do NOT introduce `Thread.sleep` for pacing — configure exponential backoff in the retry spec instead.
- Side effects between schedulers go through events: publish `DataCollectionCompletedEvent` after a collection batch, consume via `@EventListener` + `@Async`. Do not invoke signal evaluation inline from a collector.

## Package layout (root: `com.stockreport`)

- `api/` — REST controllers
- `config/` — `WebConfig`, `CacheConfig`
- `domain/` — MongoDB documents + Spring Data repositories (`stock/`, `signal/`, `favorite/`, `news/`)
- `dto/` — `request/` and `response/`
- `exception/` — `GlobalExceptionHandler`
- `scheduler/` — `@Scheduled` jobs + `StartupDataCollector` + event classes
- `service/` — business logic; `service/data/` handles collection, `service/signal/` handles evaluation

## Environment

`backend/.env` is gitignored. Required: `GEMINI_API_KEY`. Optional: `SLACK_WEBHOOK_URL` (if unset, Slack notifications no-op).
