package com.stockreport.api;

import com.stockreport.service.DataCollectionStatusService;
import com.stockreport.service.NewsService;
import com.stockreport.service.data.KrStockDataService;
import com.stockreport.service.data.UsStockDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
@Slf4j
public class SystemController {

    private final CacheManager cacheManager;
    private final KrStockDataService krStockDataService;
    private final UsStockDataService usStockDataService;
    private final NewsService newsService;
    private final DataCollectionStatusService statusService;

    @GetMapping("/collection-status")
    public ResponseEntity<?> getCollectionStatus() {
        return ResponseEntity.ok(Map.of(
                "status", statusService.getStatus().name(),
                "message", statusService.getMessage(),
                "collecting", statusService.isCollecting(),
                "lastUpdated", statusService.getLastUpdated()
        ));
    }

    @GetMapping("/cache-status")
    public ResponseEntity<?> getCacheStatus() {
        Map<String, Object> cacheInfo = new HashMap<>();
        cacheManager.getCacheNames().forEach(name -> cacheInfo.put(name, Map.of("name", name)));
        return ResponseEntity.ok(Map.of("data", Map.of("caches", cacheInfo), "meta", Map.of("timestamp", Instant.now())));
    }

    @PostMapping("/refresh-cache")
    public ResponseEntity<?> refreshCache(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String timeframe) {
        log.info("Manual cache refresh triggered, type: {}, timeframe: {}", type, timeframe);
        new Thread(() -> {
            try {
                boolean doDaily   = timeframe == null || "DAILY".equalsIgnoreCase(timeframe);
                boolean doWeekly  = timeframe == null || "WEEKLY".equalsIgnoreCase(timeframe);
                boolean doMonthly = timeframe == null || "MONTHLY".equalsIgnoreCase(timeframe);

                if (type == null || type.equals("KR")) {
                    if (doDaily)   krStockDataService.fetchAndSaveKrStocks();
                    if (doWeekly)  krStockDataService.fetchAndSaveKrWeeklyStocks();
                    if (doMonthly) krStockDataService.fetchAndSaveKrMonthlyStocks();
                }
                if (type == null || type.equals("US")) {
                    if (doDaily)   usStockDataService.fetchAndSaveUsStocks();
                    if (doWeekly)  usStockDataService.fetchAndSaveUsWeeklyStocks();
                    if (doMonthly) usStockDataService.fetchAndSaveUsMonthlyStocks();
                }
                if (type == null || type.equals("NEWS")) { newsService.fetchKrNews(); newsService.fetchUsNews(); }
                cacheManager.getCacheNames().forEach(name ->
                        Objects.requireNonNull(cacheManager.getCache(name)).clear());
                log.info("Cache refresh completed");
            } catch (Exception e) {
                log.error("Cache refresh failed: {}", e.getMessage(), e);
            }
        }).start();
        return ResponseEntity.ok(Map.of("data", Map.of("status", "refresh_started"), "meta", Map.of("timestamp", Instant.now())));
    }
}
