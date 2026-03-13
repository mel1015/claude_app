package com.stockreport.domain.favorite;

import com.stockreport.domain.stock.Market;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends MongoRepository<Favorite, String> {
    boolean existsByTickerAndMarket(String ticker, Market market);
    Optional<Favorite> findByTickerAndMarket(String ticker, Market market);
    void deleteByTickerAndMarket(String ticker, Market market);
    List<Favorite> findAllByOrderByCreatedAtDesc();
}
