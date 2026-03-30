package com.stockreport.service.data;

import com.stockreport.domain.stock.Market;
import com.stockreport.domain.stock.StockDailyCache;
import com.stockreport.domain.stock.StockDailyCacheRepository;
import com.stockreport.domain.stock.Timeframe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.functions.CheckedSupplier;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class KrStockDataService {

    private final StockDailyCacheRepository stockDailyCacheRepository;
    private final IndicatorService indicatorService;

    private static final int MAX_PAGES = 5;        // 페이지당 50개 → 최대 250개 종목
    private static final int HISTORY_DAYS = 90;    // 일봉 히스토리 기간
    private static final int HISTORY_WEEKS = 104;  // 주봉 히스토리 기간 (2년)
    private static final int HISTORY_MONTHS = 60;  // 월봉 히스토리 기간 (5년)
    private static final Charset EUC_KR = Charset.forName("EUC-KR");

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    // 시세 row 패턴: 종목코드, 종목명, 나머지 td들
    private static final Pattern ROW_PATTERN = Pattern.compile(
            "code=(\\d{6})\"[^>]*class=\"tltle\">([^<]+)</a>(.*?)</tr>", Pattern.DOTALL);
    // 숫자 필드 패턴 (현재가, 거래량, 시가총액 등)
    private static final Pattern NUM_PATTERN = Pattern.compile("class=\"number\">([0-9,]+)");
    // 등락률 패턴
    private static final Pattern PCT_PATTERN = Pattern.compile("([\\+\\-]?[\\d\\.]+)%");
    // sise.nhn XML 아이템 패턴: date|open|high|low|close|volume
    private static final Pattern OHLCV_PATTERN = Pattern.compile(
            "data=\"(\\d{8})\\|(\\d+)\\|(\\d+)\\|(\\d+)\\|(\\d+)\\|(\\d+)\"");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // 외부 API 유량 제어: 초당 10회 제한 (Thread.sleep 대체)
    private final RateLimiter naverRateLimiter = RateLimiter.of("naverApi",
            RateLimiterConfig.custom()
                    .limitForPeriod(10)
                    .limitRefreshPeriod(Duration.ofSeconds(1))
                    .timeoutDuration(Duration.ofSeconds(2))
                    .build());

    // 서킷 브레이커: 10회 슬라이딩 윈도우 중 50% 실패 시 30초 OPEN
    private final CircuitBreaker naverCircuitBreaker = CircuitBreaker.of("naverApi",
            CircuitBreakerConfig.custom()
                    .slidingWindowSize(10)
                    .failureRateThreshold(50)
                    .waitDurationInOpenState(Duration.ofSeconds(30))
                    .permittedNumberOfCallsInHalfOpenState(3)
                    .build());

    // 재시도: 최대 3회, 지수 백오프 (500ms → 1s → 2s)
    private final Retry naverRetry = Retry.of("naverApi",
            RetryConfig.custom()
                    .maxAttempts(3)
                    .intervalFunction(IntervalFunction.ofExponentialBackoff(500, 2))
                    .build());

    public void fetchAndSaveKrStocks() {
        fetchAndSaveKrStocksByTimeframe(Timeframe.DAILY, "day", HISTORY_DAYS);
    }

    public void fetchAndSaveKrWeeklyStocks() {
        fetchAndSaveKrStocksByTimeframe(Timeframe.WEEKLY, "week", HISTORY_WEEKS);
    }

    public void fetchAndSaveKrMonthlyStocks() {
        fetchAndSaveKrStocksByTimeframe(Timeframe.MONTHLY, "month", HISTORY_MONTHS);
    }

    private void fetchAndSaveKrStocksByTimeframe(Timeframe timeframe, String navTimeframe, int count) {
        log.info("Starting KR stock data fetch from Naver Finance ({})", timeframe);
        LocalDate today = LocalDate.now();
        try {
            fetchMarket(Market.KOSPI, "0", today, timeframe, navTimeframe, count);
            fetchMarket(Market.KOSDAQ, "1", today, timeframe, navTimeframe, count);
            log.info("KR stock data fetch completed ({})", timeframe);
        } catch (Exception e) {
            log.error("Failed to fetch KR stock data ({}): {}", timeframe, e.getMessage(), e);
        }
    }

    private void fetchMarket(Market market, String sosok, LocalDate today,
                             Timeframe timeframe, String navTimeframe, int count) throws Exception {
        // 오늘 데이터가 이미 수집되어 있고, 장 마감(15:30 KST) 이후 수집된 경우에만 skip
        // (서버 시작 시점과 무관하게 15:30 이후 수집 = 확정 종가)
        if (timeframe == Timeframe.DAILY) {
            StockDailyCache latest = stockDailyCacheRepository
                    .findFirstByMarketAndTimeframeOrderByTradeDateDesc(market, timeframe)
                    .orElse(null);
            if (latest != null && today.equals(latest.getTradeDate())) {
                LocalDateTime collectedAt = latest.getCollectedAt();
                LocalTime marketClose = LocalTime.of(15, 30);
                boolean collectedAfterClose = collectedAt != null &&
                        collectedAt.atZone(ZoneId.of("Asia/Seoul")).toLocalTime().isAfter(marketClose);
                if (collectedAfterClose) {
                    log.info("[{}][{}] 장 마감 후 수집된 확정 종가 데이터 존재, skip", market, timeframe);
                    return;
                }
                log.info("[{}][{}] 오늘 데이터 있으나 장 마감 전 수집분, 재수집", market, timeframe);
            }
        }

        List<StockInfo> stocks = fetchStockList(sosok, today);
        log.info("Fetched {} {} stocks from Naver Finance market summary", stocks.size(), market);

        int saved = 0;
        for (StockInfo info : stocks) {
            try {
                boolean fetched = processStock(info, market, today, timeframe, navTimeframe, count);
                if (fetched) {
                    saved++;
                }
            } catch (Exception e) {
                log.warn("Failed to process {}: {}", info.code, e.getMessage());
            }
        }
        log.info("Saved {}/{} {} stocks", saved, stocks.size(), market);
    }

    /** @return API 호출이 실제로 발생했으면 true */
    private boolean processStock(StockInfo info, Market market, LocalDate today,
                              Timeframe timeframe, String navTimeframe, int count) throws Exception {
        // 오늘 데이터가 장 마감(15:30 KST) 이후 수집된 확정 종가면 skip
        StockDailyCache existing = stockDailyCacheRepository
                .findByTickerAndMarketAndTradeDateAndTimeframe(info.code, market, today, timeframe)
                .orElse(null);
        if (existing != null) {
            LocalDateTime collectedAt = existing.getCollectedAt();
            LocalTime marketClose = LocalTime.of(15, 30);
            boolean collectedAfterClose = collectedAt != null &&
                    collectedAt.atZone(ZoneId.of("Asia/Seoul")).toLocalTime().isAfter(marketClose);
            if (collectedAfterClose) return false;
        }

        // 히스토리 여부에 따라 fetch count 결정 (처음 수집이면 full, 이후엔 최신 5개면 충분)
        boolean hasHistory = stockDailyCacheRepository
                .findFirstByTickerAndMarketAndTimeframeOrderByTradeDateDesc(info.code, market, timeframe)
                .isPresent();
        int fetchCount = hasHistory ? 5 : count;

        List<OhlcvRow> history = fetchOhlcvHistory(info.code, navTimeframe, fetchCount);
        if (history.isEmpty()) return true;

        LocalDateTime now = LocalDateTime.now();
        for (OhlcvRow row : history) {
            StockDailyCache cache = stockDailyCacheRepository
                    .findByTickerAndMarketAndTradeDateAndTimeframe(info.code, market, row.date, timeframe)
                    .orElseGet(() -> StockDailyCache.builder()
                            .ticker(info.code).market(market).name(info.name)
                            .tradeDate(row.date).timeframe(timeframe).build());

            // 과거 데이터는 이미 있으면 skip (오늘 데이터는 항상 갱신)
            if (cache.getId() != null && !row.date.equals(today)) continue;

            double changeRate = row.date.equals(today) ? info.changeRate : calcChangeRate(row, history);
            cache.setName(info.name);
            cache.setOpenPrice(row.open);
            cache.setHighPrice(row.high);
            cache.setLowPrice(row.low);
            cache.setClosePrice(row.close);
            cache.setVolume(row.volume);
            cache.setChangeRate(changeRate);
            cache.setMarketCap(row.date.equals(today) && timeframe == Timeframe.DAILY ? info.marketCap : null);
            cache.setCollectedAt(now);
            stockDailyCacheRepository.save(cache);
        }

        LocalDate latestDate = history.stream()
                .map(r -> r.date).max(LocalDate::compareTo).orElse(today);

        stockDailyCacheRepository
                .findByTickerAndMarketAndTradeDateAndTimeframe(info.code, market, latestDate, timeframe)
                .ifPresent(latest -> {
                    List<StockDailyCache> hist = stockDailyCacheRepository
                            .findByTickerAndMarketAndTimeframeOrderByTradeDateDesc(info.code, market, timeframe,
                                    PageRequest.of(0, count));
                    indicatorService.calculateAndSetIndicators(hist, latest);
                    stockDailyCacheRepository.save(latest);
                });
        return true;
    }

    /**
     * Naver Finance sise_market_sum 페이지에서 종목 목록 추출
     * sosok=0: KOSPI, sosok=1: KOSDAQ
     */
    private List<StockInfo> fetchStockList(String sosok, LocalDate today) throws Exception {
        List<StockInfo> result = new ArrayList<>();

        for (int page = 1; page <= MAX_PAGES; page++) {
            String url = "https://finance.naver.com/sise/sise_market_sum.naver?sosok=" + sosok + "&page=" + page;
            String html = fetchAsEucKr(url);

            Matcher rowMatcher = ROW_PATTERN.matcher(html);
            boolean foundAny = false;

            while (rowMatcher.find()) {
                foundAny = true;
                String code = rowMatcher.group(1);
                String name = rowMatcher.group(2).trim();
                String rest = rowMatcher.group(3);

                // td.number 필드: [현재가, 액면가, 거래량, 시가총액(억), ...]
                List<String> nums = new ArrayList<>();
                Matcher numMatcher = NUM_PATTERN.matcher(rest);
                while (numMatcher.find()) nums.add(numMatcher.group(1));

                Matcher pctMatcher = PCT_PATTERN.matcher(rest);
                double changeRate = pctMatcher.find() ? parseDouble(pctMatcher.group(1)) : 0.0;

                // td.number: [0]=현재가, [1]=액면가, [2]=거래량, [3]=시가총액(억) — 0·2는 OHLCV에서 수집
                double marketCap = nums.size() > 3 ? parseDouble(nums.get(3)) * 100_000_000L : 0; // 억원→원

                result.add(new StockInfo(code, name, changeRate, marketCap));
            }

            if (!foundAny) {
                log.debug("No more data at page {}", page);
                break;
            }
        }
        return result;
    }

    /**
     * Naver fchart sise.nhn에서 90일 OHLCV XML 파싱
     * 형식: <item data="yyyyMMdd|open|high|low|close|volume" />
     */
    private List<OhlcvRow> fetchOhlcvHistory(String code, String navTimeframe, int count) throws Exception {
        String url = "https://fchart.stock.naver.com/sise.nhn?symbol=" + code
                + "&timeframe=" + navTimeframe + "&count=" + count + "&requestType=0";
        String xml = fetchAsEucKr(url);

        List<OhlcvRow> rows = new ArrayList<>();
        Matcher m = OHLCV_PATTERN.matcher(xml);
        while (m.find()) {
            LocalDate date = LocalDate.parse(m.group(1), DATE_FMT);
            rows.add(new OhlcvRow(
                    date,
                    Double.parseDouble(m.group(2)), // open
                    Double.parseDouble(m.group(3)), // high
                    Double.parseDouble(m.group(4)), // low
                    Double.parseDouble(m.group(5)), // close
                    Long.parseLong(m.group(6))      // volume
            ));
        }
        return rows;
    }

    private String fetchAsEucKr(String url) throws Exception {
        naverRateLimiter.acquirePermission(); // 초당 10회 요청 속도 제한
        CheckedSupplier<String> decorated = Retry.decorateCheckedSupplier(naverRetry,
                CircuitBreaker.decorateCheckedSupplier(naverCircuitBreaker, () -> {
                    Request request = new Request.Builder()
                            .url(url)
                            .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                            .header("Referer", "https://finance.naver.com")
                            .header("Accept-Language", "ko-KR,ko;q=0.9")
                            .build();
                    try (Response response = httpClient.newCall(request).execute()) {
                        if (!response.isSuccessful() || response.body() == null) {
                            throw new RuntimeException("HTTP " + response.code() + " for " + url);
                        }
                        return new String(response.body().bytes(), EUC_KR);
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

    /** 전일 종가 대비 등락률 계산 */
    private double calcChangeRate(OhlcvRow row, List<OhlcvRow> history) {
        // history는 날짜 오름차순
        for (int i = 1; i < history.size(); i++) {
            if (history.get(i).date.equals(row.date) && history.get(i - 1).close > 0) {
                return (row.close - history.get(i - 1).close) / history.get(i - 1).close * 100;
            }
        }
        return 0.0;
    }

    private double parseDouble(String v) {
        try { return Double.parseDouble(v.replace(",", "").trim()); }
        catch (Exception e) { return 0.0; }
    }

    // ── 내부 데이터 클래스 ──────────────────────────────────────────────────

    private static class StockInfo {
        final String code, name;
        final double changeRate, marketCap;

        StockInfo(String code, String name, double changeRate, double marketCap) {
            this.code = code; this.name = name;
            this.changeRate = changeRate; this.marketCap = marketCap;
        }
    }

    private static class OhlcvRow {
        final LocalDate date;
        final double open, high, low, close;
        final long volume;

        OhlcvRow(LocalDate date, double open, double high, double low, double close, long volume) {
            this.date = date; this.open = open; this.high = high;
            this.low = low; this.close = close; this.volume = volume;
        }
    }
}
