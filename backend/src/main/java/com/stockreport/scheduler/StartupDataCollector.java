package com.stockreport.scheduler;

import com.stockreport.service.NewsService;
import com.stockreport.service.data.KrStockDataService;
import com.stockreport.service.data.UsStockDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

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

        @Async
        public void collect() {
            try {
                log.info("[수집 시작] 한국 주식 데이터");
                krStockDataService.fetchAndSaveKrStocks();
                log.info("[수집 완료] 한국 주식 데이터");
            } catch (Exception e) {
                log.error("[수집 실패] 한국 주식 데이터: {}", e.getMessage());
            }

            try {
                log.info("[수집 시작] 미국 주식 데이터");
                usStockDataService.fetchAndSaveUsStocks();
                log.info("[수집 완료] 미국 주식 데이터");
            } catch (Exception e) {
                log.error("[수집 실패] 미국 주식 데이터: {}", e.getMessage());
            }

            try {
                log.info("[수집 시작] 뉴스");
                newsService.fetchKrNews();
                newsService.fetchUsNews();
                newsService.deleteOldNews();
                log.info("[수집 완료] 뉴스");
            } catch (Exception e) {
                log.error("[수집 실패] 뉴스: {}", e.getMessage());
            }

            log.info("시작 시 데이터 수집 완료.");
        }
    }
}
