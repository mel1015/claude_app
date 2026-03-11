package com.stockreport.service.data;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

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

        // 오늘(ET) 데이터가 이미 수집되어 있으면 전체 skip
        if (timeframe == Timeframe.DAILY) {
            LocalDate today = LocalDate.now(ZoneId.of("America/New_York"));
            LocalDate latestNyse = stockDailyCacheRepository
                    .findFirstByMarketAndTimeframeOrderByTradeDateDesc(Market.NYSE, timeframe)
                    .map(StockDailyCache::getTradeDate).orElse(null);
            LocalDate latestNasdaq = stockDailyCacheRepository
                    .findFirstByMarketAndTimeframeOrderByTradeDateDesc(Market.NASDAQ, timeframe)
                    .map(StockDailyCache::getTradeDate).orElse(null);
            if (today.equals(latestNyse) && today.equals(latestNasdaq)) {
                log.info("[US][{}] 오늘 데이터 이미 수집됨, skip", timeframe);
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
                    Thread.sleep(100); // rate limiting (API 호출이 있을 때만)
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("Failed to fetch data for {}: {}", ticker, e.getMessage());
                failed++;
            }
        }
        log.info("US stock fetch completed ({}). Success: {}, Failed: {}", timeframe, success, failed);
    }

    /** @return API 호출이 실제로 발생했으면 true */
    private boolean fetchStockData(String ticker, String interval, String range, Timeframe timeframe) throws Exception {
        // 오늘 데이터 이미 있으면 API 호출 없이 skip
        if (timeframe == Timeframe.DAILY) {
            LocalDate today = LocalDate.now(ZoneId.of("America/New_York"));
            boolean alreadyHas = stockDailyCacheRepository
                    .findByTickerAndMarketAndTradeDateAndTimeframe(ticker, Market.NYSE, today, timeframe).isPresent()
                    || stockDailyCacheRepository
                    .findByTickerAndMarketAndTradeDateAndTimeframe(ticker, Market.NASDAQ, today, timeframe).isPresent();
            if (alreadyHas) return false;

            // 히스토리 있으면 range를 최소로 줄임 (처음 수집이면 full range 유지)
            boolean hasHistory = stockDailyCacheRepository
                    .findFirstByTickerAndMarketAndTimeframeOrderByTradeDateDesc(ticker, Market.NYSE, timeframe).isPresent()
                    || stockDailyCacheRepository
                    .findFirstByTickerAndMarketAndTimeframeOrderByTradeDateDesc(ticker, Market.NASDAQ, timeframe).isPresent();
            if (hasHistory) range = "5d";
        }

        String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + ticker
                + "?range=" + range + "&interval=" + interval;
        Request request = new Request.Builder().url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept", "application/json").build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.warn("Yahoo Finance request failed for {}: {}", ticker, response.code());
                return false;
            }
            parseAndSaveYahooData(ticker, response.body().string(), timeframe);
        }
        return true;
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

        String exchangeName = meta.path("exchangeName").asText("NASDAQ");
        String name = meta.path("shortName").asText(ticker);
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

        for (int i = 0; i < entries.size(); i++) {
            StockDailyCache stock = entries.get(i);
            Optional<StockDailyCache> existing = stockDailyCacheRepository
                    .findByTickerAndMarketAndTradeDateAndTimeframe(stock.getTicker(), market, stock.getTradeDate(), timeframe);
            if (existing.isPresent()) {
                copyFields(stock, existing.get());
                if (i == entries.size() - 1) indicatorService.calculateAndSetIndicators(entries, existing.get());
                stockDailyCacheRepository.save(existing.get());
            } else {
                if (i == entries.size() - 1) indicatorService.calculateAndSetIndicators(entries, stock);
                stockDailyCacheRepository.save(stock);
            }
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
