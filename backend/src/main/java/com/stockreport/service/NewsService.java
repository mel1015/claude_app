package com.stockreport.service;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.stockreport.domain.news.NewsCache;
import com.stockreport.domain.news.NewsCacheRepository;
import com.stockreport.dto.response.NewsDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NewsService {

    private final NewsCacheRepository newsCacheRepository;

    public Page<NewsDto> getNews(String market, String ticker, Pageable pageable) {
        Page<NewsCache> news;
        if (ticker != null && !ticker.isEmpty()) {
            news = newsCacheRepository.findByRelatedTickerOrderByPublishedAtDesc(ticker, pageable);
        } else if (market != null && !market.equals("ALL")) {
            news = newsCacheRepository.findByMarketOrderByPublishedAtDesc(market, pageable);
        } else {
            news = newsCacheRepository.findAllByOrderByPublishedAtDesc(pageable);
        }
        return news.map(this::toDto);
    }

    public List<NewsDto> getLatestNews(int limit) {
        return newsCacheRepository.findAllByOrderByPublishedAtDesc(Pageable.ofSize(limit))
                .getContent().stream().map(this::toDto).toList();
    }

    @Transactional
    public void fetchKrNews() {
        log.info("Fetching KR news...");
        List<String> rssUrls = List.of(
                "https://stock.mk.co.kr/rss/",
                "https://www.hankyung.com/feed/economy"
        );
        for (String rssUrl : rssUrls) {
            try { fetchRssFeed(rssUrl, "KR"); }
            catch (Exception e) { log.warn("Failed to fetch KR RSS {}: {}", rssUrl, e.getMessage()); }
        }
    }

    @Transactional
    public void fetchUsNews() {
        log.info("Fetching US news...");
        List<String> rssUrls = List.of(
                "https://feeds.finance.yahoo.com/rss/2.0/headline?s=^GSPC&region=US&lang=en-US",
                "https://feeds.reuters.com/reuters/businessNews"
        );
        for (String rssUrl : rssUrls) {
            try { fetchRssFeed(rssUrl, "US"); }
            catch (Exception e) { log.warn("Failed to fetch US RSS {}: {}", rssUrl, e.getMessage()); }
        }
    }

    private void fetchRssFeed(String feedUrl, String market) throws Exception {
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(new URL(feedUrl)));
        List<NewsCache> toSave = new ArrayList<>();

        for (SyndEntry entry : feed.getEntries()) {
            LocalDateTime publishedAt = entry.getPublishedDate() != null
                    ? entry.getPublishedDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
                    : LocalDateTime.now();
            String url = entry.getLink();
            if (url == null) continue;
            if (newsCacheRepository.existsByUrlAndPublishedAt(url, publishedAt)) continue;

            String summary = "";
            if (entry.getDescription() != null) {
                summary = entry.getDescription().getValue().replaceAll("<[^>]*>", "").trim();
                if (summary.length() > 500) summary = summary.substring(0, 500);
            }

            toSave.add(NewsCache.builder()
                    .title(entry.getTitle()).summary(summary).url(url)
                    .source(feed.getTitle()).market(market)
                    .publishedAt(publishedAt).fetchedAt(LocalDateTime.now())
                    .build());
        }
        newsCacheRepository.saveAll(toSave);
        log.info("Saved {} {} news items", toSave.size(), market);
    }

    @Transactional
    public void deleteOldNews() {
        newsCacheRepository.deleteByFetchedAtBefore(LocalDateTime.now().minusDays(7));
    }

    private NewsDto toDto(NewsCache news) {
        return NewsDto.builder()
                .id(news.getId()).title(news.getTitle()).summary(news.getSummary())
                .url(news.getUrl()).source(news.getSource()).market(news.getMarket())
                .relatedTicker(news.getRelatedTicker()).publishedAt(news.getPublishedAt())
                .build();
    }
}
