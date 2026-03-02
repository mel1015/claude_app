package com.stockreport.domain.news;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface NewsCacheRepository extends MongoRepository<NewsCache, String> {

    Page<NewsCache> findByMarketOrderByPublishedAtDesc(String market, Pageable pageable);

    Page<NewsCache> findByRelatedTickerOrderByPublishedAtDesc(String ticker, Pageable pageable);

    boolean existsByUrlAndPublishedAt(String url, LocalDateTime publishedAt);

    void deleteByFetchedAtBefore(LocalDateTime cutoff);

    Page<NewsCache> findAllByOrderByPublishedAtDesc(Pageable pageable);
}
