package com.stockreport.scheduler;

import com.stockreport.service.data.UsStockDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class UsDataScheduler {

    private final UsStockDataService usStockDataService;

    @Scheduled(cron = "0 0 7 * * TUE-SAT", zone = "Asia/Seoul")
    public void fetchUsDailyData() {
        log.info("US daily data scheduler started");
        usStockDataService.fetchAndSaveUsStocks();
    }
}
