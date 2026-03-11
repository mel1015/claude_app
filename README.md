# 일일 주식 리포트 (Daily Stock Report)

한국(KOSPI/KOSDAQ) + 미국(NYSE/NASDAQ) 주식 데이터를 기반으로 즐겨찾기, 거래량 TOP10, 주요 뉴스, 커스텀 시그널 기능을 제공하는 일일 리포트 웹사이트입니다.

## 기술 스택

| 구분 | 기술 |
|------|------|
| Backend | Spring Boot 3.2.3 + Java 17 |
| Database | MongoDB 7 (Docker) |
| Frontend | Next.js 14 + TypeScript + Tailwind CSS |
| 데이터 | Naver Finance (한국주식), Yahoo Finance (미국주식), RSS (뉴스) |
| 지표 | ta4j (RSI, MACD, 이동평균 MA5/10/20/60) |
| AI 분석 | Gemini 2.5 Flash (시그널 전략 검토 / 자연어 조건 생성) |

## 사전 요구사항

- Java 17+ (`java -version` 확인)
- Node.js 18+ (`node -v` 확인)
- Docker (`docker -v` 확인)

## 시작하기

### 1. MongoDB 실행 (Docker)

```bash
# Docker Desktop 실행 (꺼져 있을 경우)
open -a Docker

# 컨테이너가 없을 때 (최초 1회)
docker run -d --name mongo-stockreport -p 27017:27017 --restart unless-stopped mongo:7

# 컨테이너가 이미 있을 때
docker start mongo-stockreport
```

### 2. 환경변수 설정

`backend/.env` 파일을 생성하고 API 키를 입력합니다.

```bash
# backend/.env
GEMINI_API_KEY=your_gemini_api_key_here
```

Gemini API 키는 [Google AI Studio](https://aistudio.google.com/apikey)에서 무료로 발급받을 수 있습니다.

### 3. 백엔드 실행

```bash
cd backend

# 첫 실행 시 Gradle 8.6 자동 다운로드 후 빌드됩니다 (2-3분 소요)
GEMINI_API_KEY=your_key ./gradlew bootRun
```

백엔드가 시작되면 `http://localhost:8080` 에서 실행됩니다.

### 4. 초기 데이터 수집

백엔드가 시작되면 **일봉 데이터를 자동으로 백그라운드 수집**합니다. 프론트엔드 상단 배너로 수집 진행 상황을 확인할 수 있습니다.

```bash
# 수집 상태 확인
curl http://localhost:8080/api/v1/system/collection-status
# {"status":"COLLECTING","collecting":true,"message":"한국 주식 데이터 수집 중..."}
```

최초 수집(DB가 비어있는 경우) 시 일봉 KR 약 10분, US 약 15분이 소요됩니다. 이후 재시작 시에는 당일 데이터가 이미 있으면 즉시 skip됩니다.

주봉/월봉 데이터는 수동으로 수집합니다.

```bash
# 주봉 데이터 수집
curl -X POST "http://localhost:8080/api/v1/system/refresh-cache?type=KR&timeframe=WEEKLY"
curl -X POST "http://localhost:8080/api/v1/system/refresh-cache?type=US&timeframe=WEEKLY"

# 월봉 데이터 수집
curl -X POST "http://localhost:8080/api/v1/system/refresh-cache?type=KR&timeframe=MONTHLY"
curl -X POST "http://localhost:8080/api/v1/system/refresh-cache?type=US&timeframe=MONTHLY"
```

### 5. 프론트엔드 실행

```bash
cd frontend

# 의존성 설치 (최초 1회)
npm install

# 개발 서버 시작
npm run dev
```

프론트엔드가 시작되면 `http://localhost:3000` 에서 실행됩니다.

## 주요 기능

| 기능 | 경로 | 설명 |
|------|------|------|
| 대시보드 | `/` | 시장 요약, 즐겨찾기, 거래량TOP, 뉴스 통합 뷰 |
| 주식 검색 | `/stocks` | 전체 종목 목록, 검색, 정렬 |
| 종목 상세 | `/stocks/[ticker]` | OHLCV, 기술지표, 가격 차트 (30/60/90일) |
| 즐겨찾기 | `/favorites` | 즐겨찾기 종목 관리 |
| 거래량 TOP10 | `/top-volume` | 한국/미국 거래량 상위 종목 |
| 시그널 | `/signals` | 커스텀 조건식 시그널 목록 및 실행 |
| 시그널 빌더 | `/signals/builder` | 일봉/주봉/월봉 기준 AND/OR 조건 조합, Gemini AI 자연어 생성 및 전략 분석 |
| 뉴스 | `/news` | 한국/미국 주식 관련 최신 뉴스 |

## API 엔드포인트

```
GET  /api/v1/dashboard                         # 대시보드 통합 데이터
GET  /api/v1/stocks?market=ALL&page=0&size=20  # 주식 목록/검색
GET  /api/v1/stocks/{market}/{ticker}          # 주식 상세
GET  /api/v1/stocks/{market}/{ticker}/history  # 가격 히스토리
GET  /api/v1/stocks/top-volume?market=KR       # 거래량 TOP10
GET  /api/v1/favorites                         # 즐겨찾기 목록
POST /api/v1/favorites                         # 즐겨찾기 추가
DELETE /api/v1/favorites/{id}                  # 즐겨찾기 삭제
GET  /api/v1/signals                           # 시그널 목록
POST /api/v1/signals                           # 시그널 생성
PUT  /api/v1/signals/{id}                      # 시그널 수정
DELETE /api/v1/signals/{id}                    # 시그널 삭제
POST /api/v1/signals/{id}/run                  # 시그널 즉시 실행
POST /api/v1/signals/analyze                   # Gemini AI 전략 분석
POST /api/v1/signals/parse-text                # 자연어 → 시그널 조건 변환
GET  /api/v1/news?market=ALL                   # 뉴스 목록
GET  /api/v1/system/collection-status              # 데이터 수집 상태 조회
POST /api/v1/system/refresh-cache?type=KR|US|NEWS&timeframe=DAILY|WEEKLY|MONTHLY
```

## 데이터 수집 스케줄

| 스케줄러 | 실행 시간 | 내용 |
|---------|---------|------|
| KrDataScheduler | 매일 18:00 KST (월-금) | KOSPI/KOSDAQ 일봉 + 기술지표 |
| UsDataScheduler | 매일 07:00 KST (화-토) | NYSE/NASDAQ 일봉 + 기술지표 |
| NewsScheduler | 4시간마다 | 한국/미국 주식 뉴스 RSS |

주봉·월봉은 자동 스케줄 없이 수동 수집(`refresh-cache?timeframe=WEEKLY|MONTHLY`)으로 갱신합니다.

## 시그널 조건 JSON 형식

```json
{
  "version": "1.0",
  "logic": "AND",
  "conditions": [
    { "id": "c1", "field": "rsi14", "operator": "<", "value": 30 },
    { "id": "c2", "field": "close_price", "operator": ">", "compareField": "ma20" },
    {
      "id": "g1", "logic": "OR",
      "conditions": [
        { "id": "c3", "field": "close_price", "operator": ">", "value": 50000 },
        { "id": "c4", "field": "volume", "operator": ">", "value": 1000000 }
      ]
    }
  ]
}
```

- **value**: 숫자와 비교 (`rsi14 < 30`)
- **compareField**: 다른 지표와 비교 (`close_price > ma20`)
- **timeframe**: 시그널 생성 시 일봉/주봉/월봉 기준 선택 가능

지원 필드: `close_price`, `open_price`, `high_price`, `low_price`, `volume`, `change_rate`, `ma5`, `ma10`, `ma20`, `ma60`, `rsi14`, `macd`, `macd_signal`, `macd_hist`

## 프로젝트 구조

```
claude_app/
├── backend/                 # Spring Boot 3 (Java 17)
│   ├── build.gradle.kts
│   ├── gradlew
│   └── src/main/java/com/stockreport/
│       ├── api/             # REST Controllers
│       ├── config/          # WebConfig, CacheConfig
│       ├── domain/          # MongoDB Documents + Repositories
│       ├── dto/             # Request/Response DTOs
│       ├── exception/       # GlobalExceptionHandler
│       ├── scheduler/       # @Scheduled 배치 작업
│       └── service/         # 비즈니스 로직
│
└── frontend/                # Next.js 14 (TypeScript)
    ├── package.json
    └── src/
        ├── app/             # App Router pages
        ├── components/      # UI 컴포넌트
        ├── hooks/           # SWR data hooks
        └── lib/             # API client, types, utils
```

## MongoDB 관리

```bash
# MongoDB 쉘 접속 (개발 DB)
docker exec -it mongo-stockreport mongosh stockreport-dev

# timeframe별 데이터 수 확인
db.stock_daily_cache.countDocuments({timeframe: "DAILY"})
db.stock_daily_cache.countDocuments({timeframe: "WEEKLY"})
db.stock_daily_cache.countDocuments({timeframe: "MONTHLY"})

# 컨테이너 중지/시작
docker stop mongo-stockreport
docker start mongo-stockreport
```

> 기여 방법 및 브랜치 전략은 [CONTRIBUTING.md](./CONTRIBUTING.md)를 참고하세요.
