package com.stockreport.domain.stock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockDailyCacheRepository extends MongoRepository<StockDailyCache, String> {

    Optional<StockDailyCache> findByTickerAndMarketAndTradeDate(String ticker, Market market, LocalDate date);

    Optional<StockDailyCache> findByTickerAndMarketAndTradeDateAndTimeframe(String ticker, Market market, LocalDate date, Timeframe timeframe);

    List<StockDailyCache> findByTickerAndMarketOrderByTradeDateDesc(String ticker, Market market, Pageable pageable);

    List<StockDailyCache> findByTickerAndMarketAndTimeframeOrderByTradeDateDesc(String ticker, Market market, Timeframe timeframe, Pageable pageable);

    List<StockDailyCache> findByMarketAndTradeDateOrderByVolumeDesc(Market market, LocalDate date, Pageable pageable);

    List<StockDailyCache> findByMarketAndTradeDateAndTimeframeOrderByVolumeDesc(Market market, LocalDate date, Timeframe timeframe, Pageable pageable);

    List<StockDailyCache> findByTradeDateOrderByVolumeDesc(LocalDate date, Pageable pageable);

    List<StockDailyCache> findByTradeDateAndTimeframeOrderByVolumeDesc(LocalDate date, Timeframe timeframe, Pageable pageable);

    Optional<StockDailyCache> findFirstByMarketOrderByTradeDateDesc(Market market);

    Optional<StockDailyCache> findFirstByMarketAndTimeframeOrderByTradeDateDesc(Market market, Timeframe timeframe);

    Page<StockDailyCache> findByMarketAndTradeDate(Market market, LocalDate tradeDate, Pageable pageable);

    Page<StockDailyCache> findByMarketAndTradeDateAndTimeframe(Market market, LocalDate tradeDate, Timeframe timeframe, Pageable pageable);

    Page<StockDailyCache> findByTimeframe(Timeframe timeframe, Pageable pageable);

    @Query("{ '$or': [{'ticker': {'$regex': ?0, '$options': 'i'}}, {'name': {'$regex': ?0, '$options': 'i'}}], 'tradeDate': ?1 }")
    Page<StockDailyCache> searchByTickerOrName(String query, LocalDate date, Pageable pageable);

    @Query("{ '$or': [{'ticker': {'$regex': ?0, '$options': 'i'}}, {'name': {'$regex': ?0, '$options': 'i'}}], 'tradeDate': ?1, 'timeframe': ?2 }")
    Page<StockDailyCache> searchByTickerOrNameAndTimeframe(String query, LocalDate date, Timeframe timeframe, Pageable pageable);
}
