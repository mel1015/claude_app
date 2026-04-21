# Backend — Spring Boot 4 / Java 25

## Package root

`com.stockreport`. Full-path example (use this shape when citing files):

```
backend/src/main/java/com/stockreport/service/data/KrStockDataService.java
```

## Layout

| Directory | Role |
|-----------|------|
| `api/` | REST controllers — `StockController`, `SignalController`, `SystemController`, `FavoriteController`, `NewsController`, `DashboardController` |
| `config/` | `WebConfig`, `CacheConfig` |
| `domain/` | MongoDB documents + Spring Data repositories (`stock/`, `signal/`, `favorite/`, `news/`) |
| `dto/` | `request/` + `response/` |
| `exception/` | `GlobalExceptionHandler` |
| `scheduler/` | `@Scheduled` batch jobs, `StartupDataCollector`, event classes |
| `service/` | Business logic; `service/data/` collection, `service/signal/` evaluation |

## Key services

| File | Role |
|------|------|
| `service/data/KrStockDataService.java` | Korea collection (Naver Finance) |
| `service/data/UsStockDataService.java` | US collection (Yahoo Finance) |
| `service/data/IndicatorService.java` | ta4j indicators (RSI, MACD, MA5/10/20/60) |
| `service/StockService.java` | Queries — uses `findFirst…OrderByTradeDateDesc` |
| `service/signal/SignalEvaluator.java` | Condition tree evaluator |
| `service/GeminiService.java` | Gemini 2.5 Flash integration |
| `service/SlackNotificationService.java` | Slack webhook (no-op if URL unset) |
| `service/DataCollectionStatusService.java` | Collection status tracking |
| `scheduler/StartupDataCollector.java` | Parallel KR/US collection on boot |
| `scheduler/SignalScheduler.java` | Runs active signals on `DataCollectionCompletedEvent` |
| `scheduler/DataCollectionCompletedEvent.java` | Event DTO for the above |

## Commands

```bash
./gradlew bootRun    # start on :8080
./gradlew test
./gradlew build
```

## Rules to check

Rules under `../.claude/rules/` auto-apply here — especially `backend-spring.md` and `mongodb-queries.md`.
