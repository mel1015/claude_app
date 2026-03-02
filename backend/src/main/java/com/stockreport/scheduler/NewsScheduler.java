package com.stockreport.scheduler;

import com.stockreport.service.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NewsScheduler {

    private final NewsService newsService;

    @Scheduled(cron = "0 0 */4 * * *")
    public void fetchNews() {
        log.info("News scheduler started");
        newsService.fetchKrNews();
        newsService.fetchUsNews();
        newsService.deleteOldNews();
    }
}
