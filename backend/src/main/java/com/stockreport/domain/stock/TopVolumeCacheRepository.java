package com.stockreport.domain.stock;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface TopVolumeCacheRepository extends MongoRepository<TopVolumeCache, String> {
    List<TopVolumeCache> findByMarketAndTradeDateOrderByRank(Market market, LocalDate date);
    void deleteByMarketAndTradeDate(Market market, LocalDate date);
    List<TopVolumeCache> findByTradeDateOrderByMarketAscRankAsc(LocalDate date);
}
