package com.stockreport.service.data;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.stockreport.domain.stock.Market;
import com.stockreport.domain.stock.StockDailyCache;
import com.stockreport.domain.stock.StockDailyCacheRepository;
import com.stockreport.domain.stock.Timeframe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.springframework.data.domain.PageRequest;

@Service
@Slf4j
@RequiredArgsConstructor
public class UsStockDataService {

    private final StockDailyCacheRepository stockDailyCacheRepository;
    private final IndicatorService indicatorService;
    private final ObjectMapper objectMapper;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    // 외부 API 유량 제어: 초당 2회 제한 (Yahoo Finance rate limit 대응)
    private final RateLimiter yahooRateLimiter = RateLimiter.of("yahooApi",
            RateLimiterConfig.custom()
                    .limitForPeriod(2)
                    .limitRefreshPeriod(Duration.ofSeconds(1))
                    .timeoutDuration(Duration.ofSeconds(3))
                    .build());

    // 서킷 브레이커: 20회 슬라이딩 윈도우 중 60% 실패 시 30초 OPEN
    // (배치 수집 특성상 일시적 실패가 많으므로 window를 크게 설정)
    private final CircuitBreaker yahooCircuitBreaker = CircuitBreaker.of("yahooApi",
            CircuitBreakerConfig.custom()
                    .slidingWindowSize(20)
                    .failureRateThreshold(60)
                    .waitDurationInOpenState(Duration.ofSeconds(30))
                    .permittedNumberOfCallsInHalfOpenState(3)
                    .build());

    // 재시도: 최대 2회, 지수 백오프 (2s → 4s) — 과도한 재시도는 rate limit 악화
    private final Retry yahooRetry = Retry.of("yahooApi",
            RetryConfig.custom()
                    .maxAttempts(2)
                    .intervalFunction(IntervalFunction.ofExponentialBackoff(2000, 2))
                    .build());

    @Transactional
    public void fetchAndSaveUsStocks() {
        fetchAndSaveUsStocksByTimeframe("1d", "3mo", Timeframe.DAILY);
    }

    @Transactional
    public void fetchAndSaveUsWeeklyStocks() {
        fetchAndSaveUsStocksByTimeframe("1wk", "1y", Timeframe.WEEKLY);
    }

    @Transactional
    public void fetchAndSaveUsMonthlyStocks() {
        fetchAndSaveUsStocksByTimeframe("1mo", "5y", Timeframe.MONTHLY);
    }

    private void fetchAndSaveUsStocksByTimeframe(String interval, String range, Timeframe timeframe) {
        log.info("Starting US stock data fetch ({})", timeframe);

        // 오늘(ET) 데이터가 장 마감(16:00 ET) 이후 수집된 확정 데이터면 전체 skip
        if (timeframe == Timeframe.DAILY) {
            LocalDate today = LocalDate.now(ZoneId.of("America/New_York"));
            StockDailyCache latestNyse = stockDailyCacheRepository
                    .findFirstByMarketAndTimeframeOrderByTradeDateDesc(Market.NYSE, timeframe).orElse(null);
            StockDailyCache latestNasdaq = stockDailyCacheRepository
                    .findFirstByMarketAndTimeframeOrderByTradeDateDesc(Market.NASDAQ, timeframe).orElse(null);
            if (isCollectedAfterUsMarketClose(latestNyse, today) && isCollectedAfterUsMarketClose(latestNasdaq, today)) {
                log.info("[US][{}] 장 마감 후 수집된 확정 데이터 존재, skip", timeframe);
                return;
            }
        }

        List<String> tickers = loadSp500Tickers();
        log.info("Loaded {} S&P500 tickers", tickers.size());
        int success = 0, failed = 0;
        for (String ticker : tickers) {
            try {
                boolean fetched = fetchStockData(ticker, interval, range, timeframe);
                if (fetched) {
                    success++;
                }
            } catch (Exception e) {
                log.warn("Failed to fetch data for {}: {}", ticker, e.getMessage());
                failed++;
            }
        }
        log.info("US stock fetch completed ({}). Success: {}, Failed: {}", timeframe, success, failed);
    }

    /** @return API 호출이 실제로 발생했으면 true */
    private boolean fetchStockData(String ticker, String interval, String range, Timeframe timeframe) throws Exception {
        // 오늘 데이터가 장 마감(16:00 ET) 이후 수집된 확정 데이터면 skip
        if (timeframe == Timeframe.DAILY) {
            LocalDate today = LocalDate.now(ZoneId.of("America/New_York"));
            StockDailyCache existingNyse = stockDailyCacheRepository
                    .findByTickerAndMarketAndTradeDateAndTimeframe(ticker, Market.NYSE, today, timeframe).orElse(null);
            StockDailyCache existingNasdaq = stockDailyCacheRepository
                    .findByTickerAndMarketAndTradeDateAndTimeframe(ticker, Market.NASDAQ, today, timeframe).orElse(null);
            StockDailyCache existingToday = existingNyse != null ? existingNyse : existingNasdaq;
            if (existingToday != null && isCollectedAfterUsMarketClose(existingToday, today)) return false;

            // 히스토리 있으면 range를 최소로 줄임 (처음 수집이면 full range 유지)
            boolean hasHistory = stockDailyCacheRepository
                    .findFirstByTickerAndMarketAndTimeframeOrderByTradeDateDesc(ticker, Market.NYSE, timeframe).isPresent()
                    || stockDailyCacheRepository
                    .findFirstByTickerAndMarketAndTimeframeOrderByTradeDateDesc(ticker, Market.NASDAQ, timeframe).isPresent();
            if (hasHistory) range = "5d";
        }

        String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + ticker
                + "?range=" + range + "&interval=" + interval;
        try {
            String responseBody = callYahooApi(url);
            parseAndSaveYahooData(ticker, responseBody, timeframe);
        } catch (io.github.resilience4j.circuitbreaker.CallNotPermittedException e) {
            log.warn("[서킷 브레이커 OPEN] Yahoo Finance 요청 차단, {} 스킵", ticker);
            return false;
        }
        return true;
    }

    /** RateLimiter → Retry → CircuitBreaker 순으로 Yahoo Finance API 호출 */
    private String callYahooApi(String url) throws Exception {
        yahooRateLimiter.acquirePermission(); // 초당 5회 요청 속도 제한
        CheckedSupplier<String> decorated = Retry.decorateCheckedSupplier(yahooRetry,
                CircuitBreaker.decorateCheckedSupplier(yahooCircuitBreaker, () -> {
                    Request request = new Request.Builder().url(url)
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                            .header("Accept", "application/json").build();
                    try (Response response = httpClient.newCall(request).execute()) {
                        if (!response.isSuccessful() || response.body() == null) {
                            throw new RuntimeException("Yahoo Finance HTTP " + response.code());
                        }
                        return response.body().string();
                    }
                }));
        try {
            return decorated.get();
        } catch (Exception e) {
            throw e;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private void parseAndSaveYahooData(String ticker, String json, Timeframe timeframe) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode result = root.path("chart").path("result");
        if (!result.isArray() || result.isEmpty()) return;

        JsonNode data = result.get(0);
        JsonNode meta = data.path("meta");
        JsonNode timestamps = data.path("timestamp");
        JsonNode quote = data.path("indicators").path("quote").get(0);
        if (!timestamps.isArray()) return;

        String exchangeName = meta.path("exchangeName").stringValue("NASDAQ");
        String name = meta.path("shortName").stringValue(ticker);
        boolean isNyse = exchangeName.contains("NYSE") || exchangeName.equals("NYQ") || exchangeName.equals("NYSEArca");
        Market market = isNyse ? Market.NYSE : Market.NASDAQ;

        List<StockDailyCache> entries = new ArrayList<>();
        for (int i = 0; i < timestamps.size(); i++) {
            LocalDate date = Instant.ofEpochSecond(timestamps.get(i).asLong())
                    .atZone(ZoneId.of("America/New_York")).toLocalDate();
            Double close = safeDouble(quote.path("close"), i);
            if (close == null) continue;

            Double prevClose = (i > 0) ? safeDouble(quote.path("close"), i - 1) : null;
            Double changeRate = (prevClose != null && prevClose != 0)
                    ? ((close - prevClose) / prevClose) * 100.0 : 0.0;

            entries.add(StockDailyCache.builder()
                    .ticker(ticker).market(market).tradeDate(date).name(name)
                    .timeframe(timeframe)
                    .openPrice(safeDouble(quote.path("open"), i))
                    .highPrice(safeDouble(quote.path("high"), i))
                    .lowPrice(safeDouble(quote.path("low"), i))
                    .closePrice(close).volume(safeLong(quote.path("volume"), i))
                    .changeRate(changeRate).build());
        }

        LocalDateTime now = LocalDateTime.now();
        for (StockDailyCache stock : entries) {
            Optional<StockDailyCache> existing = stockDailyCacheRepository
                    .findByTickerAndMarketAndTradeDateAndTimeframe(stock.getTicker(), market, stock.getTradeDate(), timeframe);
            if (existing.isPresent()) {
                copyFields(stock, existing.get());
                existing.get().setCollectedAt(now);
                stockDailyCacheRepository.save(existing.get());
            } else {
                stock.setCollectedAt(now);
                stockDailyCacheRepository.save(stock);
            }
        }

        // DB에서 충분한 히스토리를 조회하여 기술지표 계산 (KrStockDataService와 동일한 방식)
        LocalDate latestDate = entries.stream().map(StockDailyCache::getTradeDate).max(LocalDate::compareTo).orElse(null);
        if (latestDate != null) {
            stockDailyCacheRepository
                    .findByTickerAndMarketAndTradeDateAndTimeframe(ticker, market, latestDate, timeframe)
                    .ifPresent(latest -> {
                        List<StockDailyCache> hist = stockDailyCacheRepository
                                .findByTickerAndMarketAndTimeframeOrderByTradeDateDesc(ticker, market, timeframe,
                                        PageRequest.of(0, 90));
                        indicatorService.calculateAndSetIndicators(hist, latest);
                        stockDailyCacheRepository.save(latest);
                    });
        }
    }

    private Double safeDouble(JsonNode arr, int idx) {
        if (!arr.isArray() || idx >= arr.size()) return null;
        JsonNode node = arr.get(idx);
        return (node == null || node.isNull()) ? null : node.asDouble();
    }

    private Long safeLong(JsonNode arr, int idx) {
        if (!arr.isArray() || idx >= arr.size()) return null;
        JsonNode node = arr.get(idx);
        return (node == null || node.isNull()) ? null : node.asLong();
    }

    private boolean isCollectedAfterUsMarketClose(StockDailyCache cache, LocalDate today) {
        if (cache == null || !today.equals(cache.getTradeDate())) return false;
        LocalDateTime collectedAt = cache.getCollectedAt();
        if (collectedAt == null) return false;
        LocalTime collectedEt = collectedAt.atZone(ZoneId.of("America/New_York")).toLocalTime();
        return collectedEt.isAfter(LocalTime.of(16, 0));
    }

    private void copyFields(StockDailyCache src, StockDailyCache dest) {
        dest.setName(src.getName());
        dest.setOpenPrice(src.getOpenPrice()); dest.setHighPrice(src.getHighPrice());
        dest.setLowPrice(src.getLowPrice()); dest.setClosePrice(src.getClosePrice());
        dest.setVolume(src.getVolume()); dest.setChangeRate(src.getChangeRate());
    }

    private List<String> loadSp500Tickers() {
        List<String> tickers = new ArrayList<>();
        try {
            ClassPathResource resource = new ClassPathResource("data/sp500_tickers.txt");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty() && !line.startsWith("#")) tickers.add(line);
                }
            }
        } catch (Exception e) {
            log.error("Failed to load S&P500 tickers: {}", e.getMessage());
        }
        return tickers;
    }
}
