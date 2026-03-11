package com.stockreport.service;

import com.stockreport.domain.stock.Market;
import com.stockreport.domain.stock.StockDailyCache;
import com.stockreport.domain.stock.StockDailyCacheRepository;
import com.stockreport.domain.stock.Timeframe;
import com.stockreport.dto.response.StockDto;
import com.stockreport.exception.StockNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class StockService {

    private final StockDailyCacheRepository stockDailyCacheRepository;

    public Page<StockDto> getStocks(String market, String query, Pageable pageable) {
        if (query != null && !query.isEmpty()) {
            LocalDate latestDate = getLatestDate(null);
            return stockDailyCacheRepository.searchByTickerOrNameAndTimeframe(query, latestDate, Timeframe.DAILY, pageable).map(this::toDto);
        }
        if (market == null || market.equals("ALL")) {
            return stockDailyCacheRepository.findByTimeframe(Timeframe.DAILY, pageable).map(this::toDto);
        }
        Market marketEnum = Market.valueOf(market.toUpperCase());
        LocalDate latestDate = getLatestDate(marketEnum);
        return stockDailyCacheRepository.findByMarketAndTradeDateAndTimeframe(marketEnum, latestDate, Timeframe.DAILY, pageable).map(this::toDto);
    }

    public StockDto getStock(String market, String ticker) {
        Market marketEnum = Market.valueOf(market.toUpperCase());
        return stockDailyCacheRepository
                .findFirstByTickerAndMarketAndTimeframeOrderByTradeDateDesc(ticker.toUpperCase(), marketEnum, Timeframe.DAILY)
                .map(this::toDto)
                .orElseThrow(() -> new StockNotFoundException("종목을 찾을 수 없습니다: " + ticker));
    }

    public List<StockDto> getStockHistory(String market, String ticker, int days) {
        Market marketEnum = Market.valueOf(market.toUpperCase());
        Pageable pageable = PageRequest.of(0, days, Sort.by("tradeDate").descending());
        return stockDailyCacheRepository
                .findByTickerAndMarketAndTimeframeOrderByTradeDateDesc(ticker.toUpperCase(), marketEnum, Timeframe.DAILY, pageable)
                .stream().map(this::toDto).toList();
    }

    public List<StockDto> getTopVolume(String market) {
        Pageable top10 = PageRequest.of(0, 10, Sort.by("volume").descending());

        if (market == null || market.equals("ALL")) {
            LocalDate latestDate = getLatestDate(null);
            return stockDailyCacheRepository.findByTradeDateAndTimeframeOrderByVolumeDesc(latestDate, Timeframe.DAILY, top10)
                    .stream().map(this::toDto).toList();
        }

        List<Market> markets = market.equals("KR")
                ? List.of(Market.KOSPI, Market.KOSDAQ)
                : List.of(Market.NYSE, Market.NASDAQ);

        List<StockDto> result = new ArrayList<>();
        for (Market m : markets) {
            LocalDate latestDate = getLatestDate(m);
            result.addAll(stockDailyCacheRepository
                    .findByMarketAndTradeDateAndTimeframeOrderByVolumeDesc(m, latestDate, Timeframe.DAILY, top10)
                    .stream().map(this::toDto).toList());
        }
        result.sort((a, b) -> Long.compare(
                b.getVolume() != null ? b.getVolume() : 0L,
                a.getVolume() != null ? a.getVolume() : 0L));

        // ticker 중복 제거 (같은 종목이 KOSPI/KOSDAQ 양쪽에 있는 경우 volume이 큰 것 유지)
        Map<String, StockDto> deduped = new LinkedHashMap<>();
        for (StockDto s : result) {
            deduped.putIfAbsent(s.getTicker(), s);
        }
        List<StockDto> dedupeList = new ArrayList<>(deduped.values());
        return dedupeList.size() > 10 ? dedupeList.subList(0, 10) : dedupeList;
    }

    public StockDto getLatestStock(String ticker, Market market) {
        return stockDailyCacheRepository
                .findFirstByTickerAndMarketAndTimeframeOrderByTradeDateDesc(ticker, market, Timeframe.DAILY)
                .map(this::toDto)
                .orElseThrow(() -> new StockNotFoundException("종목을 찾을 수 없습니다: " + ticker));
    }

    public LocalDate getLatestDate(Market market) {
        if (market != null) {
            return stockDailyCacheRepository.findFirstByMarketAndTimeframeOrderByTradeDateDesc(market, Timeframe.DAILY)
                    .map(StockDailyCache::getTradeDate).orElse(LocalDate.now());
        }
        LocalDate krDate = stockDailyCacheRepository.findFirstByMarketAndTimeframeOrderByTradeDateDesc(Market.KOSPI, Timeframe.DAILY)
                .map(StockDailyCache::getTradeDate).orElse(LocalDate.MIN);
        LocalDate usDate = stockDailyCacheRepository.findFirstByMarketAndTimeframeOrderByTradeDateDesc(Market.NYSE, Timeframe.DAILY)
                .map(StockDailyCache::getTradeDate).orElse(LocalDate.MIN);
        return krDate.isAfter(usDate) ? krDate : usDate;
    }

    public StockDto toDto(StockDailyCache stock) {
        return StockDto.builder()
                .id(stock.getId()).ticker(stock.getTicker())
                .market(stock.getMarket() != null ? stock.getMarket().name() : null)
                .name(stock.getName()).tradeDate(stock.getTradeDate())
                .openPrice(stock.getOpenPrice()).highPrice(stock.getHighPrice())
                .lowPrice(stock.getLowPrice()).closePrice(stock.getClosePrice())
                .volume(stock.getVolume()).marketCap(stock.getMarketCap())
                .changeRate(stock.getChangeRate()).ma5(stock.getMa5())
                .ma10(stock.getMa10()).ma20(stock.getMa20()).ma60(stock.getMa60()).rsi14(stock.getRsi14())
                .macd(stock.getMacd()).macdSignal(stock.getMacdSignal()).macdHist(stock.getMacdHist())
                .build();
    }
}
