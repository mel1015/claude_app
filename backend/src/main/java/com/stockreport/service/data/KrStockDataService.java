package com.stockreport.service.data;

import com.stockreport.domain.stock.Market;
import com.stockreport.domain.stock.StockDailyCache;
import com.stockreport.domain.stock.StockDailyCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.nio.charset.Charset;
import java.time.LocalDate;
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
    private static final int HISTORY_DAYS = 90;    // 지표 계산을 위한 히스토리 기간
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

    public void fetchAndSaveKrStocks() {
        log.info("Starting KR stock data fetch from Naver Finance...");
        LocalDate today = LocalDate.now();
        try {
            fetchMarket(Market.KOSPI, "0", today);
            fetchMarket(Market.KOSDAQ, "1", today);
            log.info("KR stock data fetch completed for {}", today);
        } catch (Exception e) {
            log.error("Failed to fetch KR stock data: {}", e.getMessage(), e);
        }
    }

    private void fetchMarket(Market market, String sosok, LocalDate today) throws Exception {
        List<StockInfo> stocks = fetchStockList(sosok, today);
        log.info("Fetched {} {} stocks from Naver Finance market summary", stocks.size(), market);

        int saved = 0;
        for (StockInfo info : stocks) {
            try {
                processStock(info, market, today);
                saved++;
                Thread.sleep(80); // rate limiting
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("Failed to process {}: {}", info.code, e.getMessage());
            }
        }
        log.info("Saved {}/{} {} stocks", saved, stocks.size(), market);
    }

    private void processStock(StockInfo info, Market market, LocalDate today) throws Exception {
        // 1. sise.nhn으로 90일 OHLCV 히스토리 가져오기
        List<OhlcvRow> history = fetchOhlcvHistory(info.code);
        if (history.isEmpty()) return;

        // 2. 히스토리 각 날짜 저장 (이미 있는 날짜는 스킵)
        for (OhlcvRow row : history) {
            boolean exists = stockDailyCacheRepository
                    .findByTickerAndMarketAndTradeDate(info.code, market, row.date)
                    .isPresent();
            if (exists) continue;

            // 당일 데이터면 시세 페이지에서 가져온 changeRate, marketCap 적용
            double changeRate = row.date.equals(today) ? info.changeRate : calcChangeRate(row, history);

            StockDailyCache cache = StockDailyCache.builder()
                    .ticker(info.code).market(market).name(info.name)
                    .tradeDate(row.date)
                    .openPrice(row.open).highPrice(row.high)
                    .lowPrice(row.low).closePrice(row.close)
                    .volume(row.volume)
                    .changeRate(changeRate)
                    .marketCap(row.date.equals(today) ? info.marketCap : null)
                    .build();
            stockDailyCacheRepository.save(cache);
        }

        // 3. 최신 레코드에 기술지표 계산 적용
        LocalDate latestDate = history.stream()
                .map(r -> r.date)
                .max(LocalDate::compareTo)
                .orElse(today);

        stockDailyCacheRepository
                .findByTickerAndMarketAndTradeDate(info.code, market, latestDate)
                .ifPresent(latest -> {
                    List<StockDailyCache> hist = stockDailyCacheRepository
                            .findByTickerAndMarketOrderByTradeDateDesc(info.code, market,
                                    PageRequest.of(0, 90));
                    indicatorService.calculateAndSetIndicators(hist, latest);
                    stockDailyCacheRepository.save(latest);
                });
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

                double closePrice = nums.size() > 0 ? parseDouble(nums.get(0)) : 0;
                // nums.get(1) = 액면가 (스킵)
                long volume    = nums.size() > 2 ? parseLong(nums.get(2)) : 0;
                double marketCap = nums.size() > 3 ? parseDouble(nums.get(3)) * 100_000_000L : 0; // 억원→원

                result.add(new StockInfo(code, name, closePrice, changeRate, volume, marketCap));
            }

            if (!foundAny) {
                log.debug("No more data at page {}", page);
                break;
            }
            Thread.sleep(200);
        }
        return result;
    }

    /**
     * Naver fchart sise.nhn에서 90일 OHLCV XML 파싱
     * 형식: <item data="yyyyMMdd|open|high|low|close|volume" />
     */
    private List<OhlcvRow> fetchOhlcvHistory(String code) throws Exception {
        String url = "https://fchart.stock.naver.com/sise.nhn?symbol=" + code
                + "&timeframe=day&count=" + HISTORY_DAYS + "&requestType=0";
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

    private long parseLong(String v) {
        try { return Long.parseLong(v.replace(",", "").trim()); }
        catch (Exception e) { return 0L; }
    }

    // ── 내부 데이터 클래스 ──────────────────────────────────────────────────

    private static class StockInfo {
        final String code, name;
        final double closePrice, changeRate, marketCap;
        final long volume;

        StockInfo(String code, String name, double closePrice, double changeRate, long volume, double marketCap) {
            this.code = code; this.name = name;
            this.closePrice = closePrice; this.changeRate = changeRate;
            this.volume = volume; this.marketCap = marketCap;
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
