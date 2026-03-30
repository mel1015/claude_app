package com.stockreport.scheduler;

import com.stockreport.service.DataCollectionStatusService;
import com.stockreport.service.NewsService;
import com.stockreport.service.data.KrStockDataService;
import com.stockreport.service.data.UsStockDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
@RequiredArgsConstructor
public class StartupDataCollector implements ApplicationRunner {

    private final AsyncCollector asyncCollector;

    @Override
    public void run(ApplicationArguments args) {
        log.info("서버 시작 시 데이터 수집을 백그라운드에서 시작합니다.");
        asyncCollector.collect();
    }

    @Component
    @Slf4j
    @RequiredArgsConstructor
    public static class AsyncCollector {

        private final KrStockDataService krStockDataService;
        private final UsStockDataService usStockDataService;
        private final NewsService newsService;
        private final DataCollectionStatusService statusService;
        private final ApplicationEventPublisher eventPublisher;

        @Async
        public void collect() {
            statusService.start("KR/US 데이터 병렬 수집 중...");
            statusService.startKr();
            statusService.startUs();

            // KR + US 병렬 수집 (CompletableFuture.allOf)
            CompletableFuture<Void> krFuture = CompletableFuture.runAsync(() -> {
                try {
                    log.info("[수집 시작] 한국 주식 데이터");
                    krStockDataService.fetchAndSaveKrStocks();
                    statusService.completeKr();
                    log.info("[수집 완료] 한국 주식 데이터");
                } catch (Exception e) {
                    log.error("[수집 실패] 한국 주식 데이터: {}", e.getMessage());
                    statusService.failKr();
                }
            });

            CompletableFuture<Void> usFuture = CompletableFuture.runAsync(() -> {
                try {
                    log.info("[수집 시작] 미국 주식 데이터");
                    usStockDataService.fetchAndSaveUsStocks();
                    statusService.completeUs();
                    log.info("[수집 완료] 미국 주식 데이터");
                } catch (Exception e) {
                    log.error("[수집 실패] 미국 주식 데이터: {}", e.getMessage());
                    statusService.failUs();
                }
            });

            try {
                CompletableFuture.allOf(krFuture, usFuture).join();
            } catch (Exception e) {
                log.error("[병렬 수집 오류]: {}", e.getMessage());
            }

            try {
                log.info("[수집 시작] 뉴스");
                statusService.start("뉴스 수집 중...");
                newsService.fetchKrNews();
                newsService.fetchUsNews();
                newsService.deleteOldNews();
                log.info("[수집 완료] 뉴스");
            } catch (Exception e) {
                log.error("[수집 실패] 뉴스: {}", e.getMessage());
            }

            log.info("시작 시 데이터 수집 완료.");
            statusService.complete("데이터 수집 완료");
            eventPublisher.publishEvent(new DataCollectionCompletedEvent(this));
        }
    }
}
