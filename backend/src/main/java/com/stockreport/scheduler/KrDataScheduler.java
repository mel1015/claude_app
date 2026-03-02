package com.stockreport.scheduler;

import com.stockreport.service.data.KrStockDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class KrDataScheduler {

    private final KrStockDataService krStockDataService;

    @Scheduled(cron = "0 0 18 * * MON-FRI", zone = "Asia/Seoul")
    public void fetchKrDailyData() {
        log.info("KR daily data scheduler started");
        krStockDataService.fetchAndSaveKrStocks();
    }
}
