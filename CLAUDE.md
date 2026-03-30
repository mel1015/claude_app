# CLAUDE.md — Claude Code 코드베이스 가이드

## 프로젝트 개요

한국(KOSPI/KOSDAQ) + 미국(NYSE/NASDAQ) 주식 일일 리포트 웹사이트.

- **Backend**: Spring Boot 4.0.3 / Java 25 — `backend/`
- **Frontend**: Next.js 14 / TypeScript — `frontend/`
- **Database**: MongoDB 7 (Docker) — 컬렉션: `stock_daily_cache`, `news_cache`, `favorites`, `signals`

## 개발 환경 요구사항

- Java: **Temurin 25**
- Node.js: 18+
- Docker: MongoDB 컨테이너 실행 필요

```bash
docker start mongo-stockreport
# 최초: docker run -d --name mongo-stockreport -p 27017:27017 mongo:7
```

## 실행 명령

```bash
# 백엔드 (포트 8080)
cd backend && ./gradlew bootRun

# 프론트엔드 (포트 3000)
cd frontend && npm run dev
```

> **MongoDB 프로퍼티 변경 (Spring Boot 4)**: `spring.data.mongodb.uri` → `spring.mongodb.uri`로 변경됨. `application.yml`과 `application-dev.yml` 모두 새 키 사용.

> **프로필 분리**: `application.yml` (prod: `stockreport`), `application-dev.yml` (dev: `stockreport-dev`). `spring.profiles.active: dev`로 개발 환경 자동 적용.

## 중요 파일 위치

| 역할 | 경로 |
|------|------|
| 데이터 수집 (한국) | `backend/.../service/data/KrStockDataService.java` |
| 데이터 수집 (미국) | `backend/.../service/data/UsStockDataService.java` |
| 서버 시작 자동 수집 | `backend/.../scheduler/StartupDataCollector.java` |
| 수집 상태 관리 | `backend/.../service/DataCollectionStatusService.java` |
| 수집 완료 이벤트 | `backend/.../scheduler/DataCollectionCompletedEvent.java` |
| 주식 서비스 | `backend/.../service/StockService.java` |
| 기술지표 계산 | `backend/.../service/data/IndicatorService.java` |
| 시그널 평가 엔진 | `backend/.../service/signal/SignalEvaluator.java` |
| 시그널 자동 실행 | `backend/.../scheduler/SignalScheduler.java` |
| Gemini AI 연동 | `backend/.../service/GeminiService.java` |
| Slack 알림 | `backend/.../service/SlackNotificationService.java` |
| REST API | `backend/.../api/` (StockController, SignalController, SystemController 등) |
| 환경변수 | `backend/.env` (GEMINI_API_KEY, SLACK_WEBHOOK_URL — 절대 커밋 금지) |
| API 클라이언트 | `frontend/src/lib/api.ts` |
| 공통 타입 | `frontend/src/lib/types.ts` |
| SWR 훅 | `frontend/src/hooks/useStocks.ts` |

## 주요 아키텍처 결정

### MongoDB 도메인 모델
- `StockDailyCache`: ticker + market + tradeDate + **timeframe** 조합이 고유 키
- `timeframe` 필드 필수: `DAILY` / `WEEKLY` / `MONTHLY`
- 모든 조회에 `Timeframe.DAILY` 명시 — 없으면 WEEKLY/MONTHLY 데이터가 섞임

### 데이터 수집 최적화 (중요)
- 서버 시작 시 `StartupDataCollector`가 KR·US 수집을 `CompletableFuture.allOf()`로 **병렬** 실행
- **시장 레벨 skip**: DB 최신 날짜 == 오늘이면 전체 skip (1번 쿼리)
- **종목 레벨 skip**: 당일 데이터 있으면 API 호출 없이 skip
- **incremental fetch**: 히스토리 있으면 KR: 최근 5개, US: `5d` range만 조회
- 최초 수집 시에만 full 히스토리 가져옴 (KR: 90일, US: 3개월)

### Resilience4j 외부 API 보호
- Naver Finance / Yahoo Finance / Gemini API 호출에 `RateLimiter + CircuitBreaker + Retry` 적용
- `Thread.sleep` 제거 → 지수 백오프 재시도로 교체
- 연속 실패 시 서킷브레이커 자동 차단 (OPEN 상태) → 불필요한 API 호출 차단

### StockService 조회 패턴
- `getStock()` / `getLatestStock()` → `findFirstByTickerAndMarketAndTimeframeOrderByTradeDateDesc`
  - 날짜 불일치 문제 방지 (종목별 최신 수집 날짜가 다를 수 있음)
- `getTopVolume()` → ticker 기준 중복 제거 (`putIfAbsent`)

### 시그널 자동 실행 패턴
- `KrDataScheduler` / `UsDataScheduler` 수집 완료 시 `DataCollectionCompletedEvent` 발행
- `SignalScheduler`가 `@EventListener`로 수신 → 활성 시그널 전체 자동 실행 (`@Async`)
- 매칭 결과 있으면 `SlackNotificationService`가 Webhook으로 알림 전송 (URL 미설정 시 skip)

### Virtual Threads
- `spring.threads.virtual.enabled: true` — Java 25 가상 스레드 활성화 (application.yml)

## API 엔드포인트 요약

```
GET    /api/v1/dashboard                                              # 대시보드 통합 데이터
GET    /api/v1/stocks?market=ALL&page=0&size=20&query=삼성            # 주식 목록/검색
GET    /api/v1/stocks/{market}/{ticker}                               # 주식 상세
GET    /api/v1/stocks/{market}/{ticker}/history                       # 가격 히스토리
GET    /api/v1/stocks/top-volume?market=KR                           # 거래량 TOP10
GET    /api/v1/favorites                                              # 즐겨찾기 목록
POST   /api/v1/favorites                                              # 즐겨찾기 추가
DELETE /api/v1/favorites/{id}                                         # 즐겨찾기 삭제
GET    /api/v1/signals                                                # 시그널 목록
POST   /api/v1/signals                                                # 시그널 생성
PUT    /api/v1/signals/{id}                                           # 시그널 수정
DELETE /api/v1/signals/{id}                                           # 시그널 삭제
POST   /api/v1/signals/{id}/run                                       # 시그널 즉시 실행
POST   /api/v1/signals/analyze                                        # Gemini AI 전략 분석
POST   /api/v1/signals/parse-text                                     # 자연어 → 시그널 조건 변환
GET    /api/v1/news?market=ALL                                        # 뉴스 목록
GET    /api/v1/system/collection-status                               # 데이터 수집 상태 확인 (krStatus/usStatus 포함)
POST   /api/v1/system/refresh-cache?type=KR|US|NEWS&timeframe=DAILY|WEEKLY|MONTHLY  # 수동 수집 트리거
```

## 환경변수

```bash
# backend/.env (gitignore 등록됨)
GEMINI_API_KEY=your_key_here

# Slack 알림 (선택사항 — 미설정 시 알림 없이 정상 동작)
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/xxx/yyy/zzz
```

## 주의사항

- API 키 등 민감 정보는 절대 코드/yml에 직접 작성 금지 → `backend/.env` 사용
- 요청 기능이 현재 구조상 불가능하거나 제약이 있을 경우 임의로 처리하지 말고 "불가 이유 + 대안" 먼저 제시
- SWR fetcher에 제네릭 명시 필요: `fetchApi<T>(url)` (TypeScript 타입 오류 방지)
