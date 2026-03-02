package com.stockreport.service;

import com.stockreport.domain.stock.Market;
import com.stockreport.domain.stock.StockDailyCache;
import com.stockreport.domain.stock.StockDailyCacheRepository;
import com.stockreport.dto.response.DashboardDto;
import com.stockreport.dto.response.FavoriteDto;
import com.stockreport.dto.response.NewsDto;
import com.stockreport.dto.response.SignalDto;
import com.stockreport.dto.response.StockDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardService {

    private final StockService stockService;
    private final FavoriteService favoriteService;
    private final NewsService newsService;
    private final SignalService signalService;
    private final StockDailyCacheRepository stockDailyCacheRepository;

    public DashboardDto getDashboard() {
        List<FavoriteDto> favorites = favoriteService.getFavorites();
        List<StockDto> favoriteStocks = favorites.stream()
                .filter(f -> f.getLatestStock() != null).map(FavoriteDto::getLatestStock).toList();
        List<StockDto> topVolumeKr = stockService.getTopVolume("KR");
        List<StockDto> topVolumeUs = stockService.getTopVolume("US");
        List<NewsDto> latestNews = newsService.getLatestNews(10);
        List<SignalDto> activeSignals = signalService.getSignals().stream()
                .filter(SignalDto::isActive).limit(5).toList();

        return DashboardDto.builder()
                .tradeDate(LocalDate.now())
                .favorites(favoriteStocks)
                .topVolumeKr(topVolumeKr)
                .topVolumeUs(topVolumeUs)
                .latestNews(latestNews)
                .activeSignals(activeSignals)
                .krSummary(buildMarketSummary("KR"))
                .usSummary(buildMarketSummary("US"))
                .build();
    }

    private DashboardDto.MarketSummary buildMarketSummary(String marketGroup) {
        List<Market> markets = marketGroup.equals("KR")
                ? List.of(Market.KOSPI, Market.KOSDAQ)
                : List.of(Market.NYSE, Market.NASDAQ);

        int total = 0, rising = 0, falling = 0;
        double sumChangeRate = 0.0;

        for (Market market : markets) {
            LocalDate latestDate = stockDailyCacheRepository
                    .findFirstByMarketOrderByTradeDateDesc(market)
                    .map(com.stockreport.domain.stock.StockDailyCache::getTradeDate).orElse(LocalDate.now());
            List<StockDailyCache> stocks = stockDailyCacheRepository
                    .findByMarketAndTradeDateOrderByVolumeDesc(market, latestDate,
                            PageRequest.of(0, Integer.MAX_VALUE));
            for (StockDailyCache stock : stocks) {
                total++;
                double cr = stock.getChangeRate() != null ? stock.getChangeRate() : 0.0;
                sumChangeRate += cr;
                if (cr > 0) rising++;
                else if (cr < 0) falling++;
            }
        }

        return DashboardDto.MarketSummary.builder()
                .market(marketGroup).totalStocks(total).risingStocks(rising).fallingStocks(falling)
                .avgChangeRate(total > 0 ? sumChangeRate / total : 0.0)
                .build();
    }
}
