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
| 주식 서비스 | `backend/.../service/StockService.java` |
| 기술지표 계산 | `backend/.../service/data/IndicatorService.java` |
| REST API | `backend/.../api/` (StockController, SystemController 등) |
| 환경변수 | `backend/.env` (GEMINI_API_KEY — 절대 커밋 금지) |
| API 클라이언트 | `frontend/src/lib/api.ts` |
| 공통 타입 | `frontend/src/lib/types.ts` |
| SWR 훅 | `frontend/src/hooks/useStocks.ts` |

## 주요 아키텍처 결정

### MongoDB 도메인 모델
- `StockDailyCache`: ticker + market + tradeDate + **timeframe** 조합이 고유 키
- `timeframe` 필드 필수: `DAILY` / `WEEKLY` / `MONTHLY`
- 모든 조회에 `Timeframe.DAILY` 명시 — 없으면 WEEKLY/MONTHLY 데이터가 섞임

### 데이터 수집 최적화 (중요)
- 서버 시작 시 `StartupDataCollector`가 `@Async`로 자동 수집
- **시장 레벨 skip**: DB 최신 날짜 == 오늘이면 전체 skip (1번 쿼리)
- **종목 레벨 skip**: 당일 데이터 있으면 API 호출 없이 skip
- **incremental fetch**: 히스토리 있으면 KR: 최근 5개, US: `5d` range만 조회
- 최초 수집 시에만 full 히스토리 가져옴 (KR: 90일, US: 3개월)

### StockService 조회 패턴
- `getStock()` / `getLatestStock()` → `findFirstByTickerAndMarketAndTimeframeOrderByTradeDateDesc`
  - 날짜 불일치 문제 방지 (종목별 최신 수집 날짜가 다를 수 있음)
- `getTopVolume()` → ticker 기준 중복 제거 (`putIfAbsent`)

## API 엔드포인트 요약

```
GET  /api/v1/system/collection-status       # 데이터 수집 상태 확인
POST /api/v1/system/refresh-cache           # 수동 수집 트리거
GET  /api/v1/stocks?market=ALL&query=삼성   # 주식 목록/검색
GET  /api/v1/stocks/{market}/{ticker}       # 주식 상세
GET  /api/v1/dashboard                      # 대시보드
```

## 환경변수

```bash
# backend/.env (gitignore 등록됨)
GEMINI_API_KEY=your_key_here
```

## 주의사항

- API 키 등 민감 정보는 절대 코드/yml에 직접 작성 금지 → `backend/.env` 사용
- 요청 기능이 현재 구조상 불가능하거나 제약이 있을 경우 임의로 처리하지 말고 "불가 이유 + 대안" 먼저 제시
- SWR fetcher에 제네릭 명시 필요: `fetchApi<T>(url)` (TypeScript 타입 오류 방지)
